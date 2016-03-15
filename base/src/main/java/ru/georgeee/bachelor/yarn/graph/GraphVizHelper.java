package ru.georgeee.bachelor.yarn.graph;

import org.springframework.stereotype.Component;

import java.io.Writer;
import java.util.Optional;

@Component
public class GraphVizHelper {
    public <T, V> void toGraph(NodeRepository<T, V> left, NodeRepository<V, T> right, Appendable out) {
        double lBase = edgeBase(left);
        double rBase = edgeBase(right);
    }

    private <T, V> double edgeBase(NodeRepository<T, V> repo) {
        return repo.getNodes().stream().map(n -> n.getEdges().values()
                .stream().max(Double::compare))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .max(Double::compare)
                .orElse(1.0);
    }
}
