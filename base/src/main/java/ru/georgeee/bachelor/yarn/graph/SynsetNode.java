package ru.georgeee.bachelor.yarn.graph;

import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public abstract class SynsetNode<T, V> {
    private final Map<SynsetNode<V, T>, Double> edges = new HashMap<>();
    private final T data;

    protected SynsetNode(T data) {
        this.data = data;
    }

    public void addEdge(SynsetNode<V, T> node, double value) {
        edges.put(node, value);
    }

    public abstract String getId();

    public abstract List<String> getWords();

    public abstract POS getPOS();

    @Override
    public String toString() {
        return "{id=" + getId() + ": " + getWords() + " edges="
                + edges.entrySet().stream().map(e -> e.getKey().getId() + ": " + e.getValue()).collect(Collectors.toList())
                + "}";
    }

    public enum POS {
        NOUN, VERB, ADJECTIVE, ADVERB;
    }
}
