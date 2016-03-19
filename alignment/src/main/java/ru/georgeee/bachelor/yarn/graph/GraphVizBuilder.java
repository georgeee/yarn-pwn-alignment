package ru.georgeee.bachelor.yarn.graph;

import lombok.Getter;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GraphVizBuilder implements AutoCloseable {
    private final Appendable out;
    private final Set<SynsetNode<?, ?>> used = new HashSet<>();
    @Getter
    private final Set<SynsetNode<?, ?>> created = new HashSet<>();

    public GraphVizBuilder(Appendable out) throws IOException {
        this.out = out;
        out.append("graph synsets{\n layout = fdp;\n");
    }

    public <T, V> void addRepo(NodeRepository<T, V> repo) throws IOException {
        for (SynsetNode<T, V> from : repo.getNodes()) {
            addNode(from);
        }
    }

    public <T, V> void addNode(SynsetNode<T, V> from) throws IOException {
        if(used.add(from)) {
            for (Map.Entry<SynsetNode<V, T>, Double> e : from.getEdges().entrySet()) {
                SynsetNode<V, T> to = e.getKey();
                if (!used.contains(to)) {
                    double weight1 = e.getValue();
                    double weight2 = to.getEdges().getOrDefault(from, 0.0);
                    addEdge(from, to, weight1, weight2);
                }
            }
        }
    }


    private <T, V> void addEdge(SynsetNode<T, V> from, SynsetNode<V, T> to, double weight1, double weight2) throws IOException {
        double weight = (weight1 + weight2) / 2;
        writeNodeDef(from);
        writeNodeDef(to);
        out
                .append(gvEscapeId(from.getId()))
                .append(" -- ")
                .append(gvEscapeId(to.getId()))
                .append(" [color=grey")
                .append(String.valueOf((int) ((1 - weight) * 100)))
                .append(", label=\"")
                .append(String.format("%.2f", weight))
                .append("\"];\n");
    }

    private String gvEscapeId(String id) {
        return id.replace('-', '_');
    }

    private <T, V> void writeNodeDef(SynsetNode<T, V> node) throws IOException {
        if (!created.add(node)) return;
        out
                .append(gvEscapeId(node.getId()))
                .append(" [label=\"")
                .append(node.getId())
                .append("\"];\n");
    }

    @Override
    public void close() throws IOException {
        out.append("}");
    }
}
