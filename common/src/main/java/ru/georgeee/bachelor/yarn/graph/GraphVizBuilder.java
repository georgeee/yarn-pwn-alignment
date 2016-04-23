package ru.georgeee.bachelor.yarn.graph;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.georgeee.bachelor.yarn.clustering.Cluster;

import java.io.IOException;
import java.util.*;

public class GraphVizBuilder implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(GraphVizBuilder.class);
    private final GraphVizSettings settings;
    private final Appendable out;
    private final Set<SynsetNode<?, ?>> used = new HashSet<>();
    @Getter
    private final Set<SynsetNode<?, ?>> created = new HashSet<>();

    private final Map<SynsetNode<?, ?>, Map<SynsetNode<?, ?>, TranslationLink>> edges = new HashMap<>();

    public GraphVizBuilder(GraphVizSettings settings, Appendable out) throws IOException {
        this.settings = settings;
        this.out = out;
        out.append("graph synsets{\n layout = ").append(settings.getEngine()).append(";\n");
    }

    public void addIgnored(Collection<? extends SynsetNode<?, ?>> nodes) {
        used.addAll(nodes);
    }

    public <T, V> void addRepo(NodeRepository<T, V> repo) throws IOException {
        for (SynsetNode<T, V> from : repo.getNodes()) {
            addNode(from);
        }
    }

    private Map<? extends SynsetNode<?, ?>, TranslationLink> getEdges(SynsetNode<?, ?> from) {
        if (settings.getMaxEdges() > 0) {
            return edges.computeIfAbsent(from, k -> {
                List<Map.Entry<? extends SynsetNode<?, ?>, TranslationLink>> entries = new ArrayList<>();
                from.getEdges().entrySet().stream()
                        .filter(e -> !used.contains(e.getKey()))
                        .forEach(entries::add);
                entries.sort((a, b) -> {
                    TranslationLink arLink = a.getKey().getEdges().get(from);
                    TranslationLink brLink = b.getKey().getEdges().get(from);
                    double aWeight = a.getValue().getWeight() + (arLink == null ? 0 : arLink.getWeight());
                    double bWeight = b.getValue().getWeight() + (brLink == null ? 0 : brLink.getWeight());
                    return -Double.compare(aWeight, bWeight);
                });
                Map<SynsetNode<?, ?>, TranslationLink> map = new HashMap<>();
                entries.stream().limit(settings.getMaxEdges()).forEach(e -> map.put(e.getKey(), e.getValue()));
                return map;
            });
        } else {
            return from.getEdges();
        }
    }

    public <T, V> void addNode(SynsetNode<T, V> from) throws IOException {
        if (used.add(from)) {
            for (Map.Entry<? extends SynsetNode<?, ?>, TranslationLink> e : getEdges(from).entrySet()) {
                SynsetNode<?, ?> to = e.getKey();
                if (!used.contains(to)) {
                    TranslationLink link2 = to.getEdges().get(from);
                    if (from.getId().compareTo(to.getId()) < 0) {
                        addEdge(from, to, e.getValue(), link2);
                    } else {
                        addEdge(to, from, link2, e.getValue());
                    }
                }
            }
        }
    }


    private void addEdge(SynsetNode<?, ?> from, SynsetNode<?, ?> to, TranslationLink link1, TranslationLink link2) throws IOException {
        double weight1 = link1 == null ? 0.0 : link1.getWeight();
        double weight2 = link2 == null ? 0.0 : link2.getWeight();
        double weight = (weight1 + weight2) / 2;
        if (weight < settings.getMeanThreshold()) return;
        if (weight1 < settings.getThreshold() || weight2 < settings.getThreshold()) return;
        writeNodeDef(from);
        writeNodeDef(to);
        out
                .append(gvEscapeId(from.getId()))
                .append(" -- ")
                .append(gvEscapeId(to.getId()))
                .append(" [fontsize=8, color=grey")
                .append(String.valueOf((int) ((1 - weight) * 100)))
                .append(", label=\"")
                .append(String.format("%.2f {%.2f, %.2f}", weight, weight1, weight2))
                .append("\"];\n");
        log.info("{} -> {}: {} (word {})", from.getId(), to.getId(), link1 == null ? null : link1.getTranslationMeaning(), link1 == null ? null : link1.getWord());
        log.info("{} -> {}: {} (word {})", to.getId(), from.getId(), link2 == null ? null : link2.getTranslationMeaning(), link2 == null ? null : link2.getWord());
    }

    private String gvEscapeId(String id) {
        return id.replace('-', '_');
    }

    private void writeNodeDef(String id, String label, Set<String> words) throws IOException {
        out
                .append(gvEscapeId(id))
                .append(" [fontsize=7, label=\"")
                .append(label)
                .append('\n');
        printSeparated(out, words, '\n', 5);
        out.append("\"];\n");
    }

    private <T, V> void writeNodeDef(SynsetNode<T, V> node) throws IOException {
        if (!created.add(node)) return;
        if (node instanceof Cluster) {
            out
                    .append("subgraph cluster__")
                    .append(gvEscapeId(node.getId()))
                    .append("{\n color=blue; fontsize=7; label=\"")
                    .append(node.getId())
                    .append("\";\n");
            boolean first = true;
            for (Cluster.Member<T, V> member : ((Cluster<T, V>) node)) {
                SynsetNode<T, V> mNode = member.getNode();
                String mLabel = String.format("%s { %f : %f ; %f }", mNode.getId(), member.getMWeight(), member.getWeight(), member.getRWeight());
                writeNodeDef(first ? node.getId() : node.getId() + "--" + mNode.getId(), mLabel, mNode.getWords());
                first = false;
            }
            out.append("}");
        } else {
            writeNodeDef(node.getId(), node.getId(), node.getWords());
        }
    }

    private void printSeparated(Appendable out, Collection<String> words, char sepBy, int sepAt) throws IOException {
        out.append('[');
        int i = 0;
        for (String word : words) {
            out.append(word).append(", ");
            if (++i == sepAt) {
                out.append(sepBy);
                i = 0;
            }
        }
        out.append(']');
    }

    @Override
    public void close() throws IOException {
        out.append("}");
    }
}
