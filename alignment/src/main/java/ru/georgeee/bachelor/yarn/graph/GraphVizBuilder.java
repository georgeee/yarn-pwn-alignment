package ru.georgeee.bachelor.yarn.graph;

import lombok.Getter;

import java.io.IOException;
import java.util.*;

public class GraphVizBuilder implements AutoCloseable {
    private final GraphVizSettings settings;
    private final Appendable out;
    private final Set<SynsetNode<?, ?>> used = new HashSet<>();
    @Getter
    private final Set<SynsetNode<?, ?>> created = new HashSet<>();

    public GraphVizBuilder(GraphVizSettings settings, Appendable out) throws IOException {
        this.settings = settings;
        this.out = out;
        out.append("graph synsets{\n layout = " + settings.getEngine() + ";\n");
    }

    public <T, V> void addRepo(NodeRepository<T, V> repo) throws IOException {
        for (SynsetNode<T, V> from : repo.getNodes()) {
            addNode(from);
        }
    }

    public <T, V> void addNode(SynsetNode<T, V> from) throws IOException {
        if (used.add(from)) {
            for (Map.Entry<SynsetNode<V, T>, Double> e : from.getEdges().entrySet()) {
                SynsetNode<V, T> to = e.getKey();
                if (!used.contains(to)) {
                    double weight1 = e.getValue();
                    double weight2 = to.getEdges().getOrDefault(from, 0.0);
                    if (from.getId().compareTo(to.getId()) < 0) {
                        addEdge(from, to, weight1, weight2);
                    } else {
                        addEdge(to, from, weight2, weight1);
                    }
                }
            }
        }
    }


    private <T, V> void addEdge(SynsetNode<T, V> from, SynsetNode<V, T> to, double weight1, double weight2) throws IOException {
        double weight = (weight1 + weight2) / 2;
        if (weight < settings.getThreshold()) return;
        writeNodeDef(from);
        writeNodeDef(to);
        out
                .append(gvEscapeId(from.getId()))
                .append(" -- ")
                .append(gvEscapeId(to.getId()))
                .append(" [fontsize=8, color=grey")
                .append(String.valueOf((int) ((1 - weight) * 100)))
                .append(", label=\"")
                .append(String.format("%.2f [%.2f, %.2f]", weight, weight1, weight2))
                .append("\"];\n");
    }

    private String gvEscapeId(String id) {
        return id.replace('-', '_');
    }

    private <T, V> void writeNodeDef(SynsetNode<T, V> node) throws IOException {
        if (!created.add(node)) return;
        out
                .append(gvEscapeId(node.getId()))
                .append(" [fontsize=7, label=\"")
                .append(node.getId())
                .append('\n');
        printSeparated(out, node.getWords(), '\n', 5);
        out.append("\"];\n");
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
