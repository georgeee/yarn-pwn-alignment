package ru.georgeee.bachelor.yarn.graph;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

@Component
public class GraphVizHelper {
    public <T, V> void toGraph(NodeRepository<T, V> left, NodeRepository<V, T> right, Appendable out) throws IOException {
        out.append("digraph synsets{\n layout = fdp;\n");
        addNodeDefs(true, left.getNodes(), out);
        addNodeDefs(false, right.getNodes(), out);
        addEdgeDefs(true, left.getNodes(), out);
        addEdgeDefs(false, right.getNodes(), out);
        out.append("}");
    }

    private <T, V> void addEdgeDefs(boolean isLeft, Collection<SynsetNode<T, V>> nodes, Appendable out) throws IOException {
        for (SynsetNode<T, V> from : nodes) {
            for (Map.Entry<SynsetNode<V, T>, Double> e : from.getEdges().entrySet()) {
                SynsetNode<V, T> to = e.getKey();
                double weight = e.getValue();
                out.append(isLeft ? "L_" : "R_")
                        .append(gvEscapeId(from.getId()))
                        .append(" -> ")
                        .append(isLeft ? "R_" : "L_")
                        .append(gvEscapeId(to.getId()))
                        .append(" [color=grey")
                        .append(String.valueOf((int) ((1 - weight) * 100)))
                        .append(", label=\"")
                        .append(String.valueOf(weight))
                        .append("\"];\n");
            }
        }
    }

    private String gvEscapeId(String id){
        return id.replace('-', '_');
    }

    private <T, V> void addNodeDefs(boolean isLeft, Iterable<SynsetNode<T, V>> nodes, Appendable out) throws IOException {
        for (SynsetNode<T, V> node : nodes) {
            out.append(isLeft ? "L_" : "R_")
                    .append(gvEscapeId(node.getId()))
                    .append(" [label=\"")
                    .append(isLeft ? "L " : "R ")
                    .append(node.getId())
                    .append("\", color=")
                    .append(isLeft ? "yellow" : "blue")
                    .append("];\n");
        }
    }

}
