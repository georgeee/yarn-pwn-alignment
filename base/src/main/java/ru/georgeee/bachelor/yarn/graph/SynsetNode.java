package ru.georgeee.bachelor.yarn.graph;

import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Getter
public abstract class SynsetNode<T, V> {
    private final Map<SynsetNode<V, T>, Double> edges = new HashMap<>();
    private final T data;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SynsetNode<?, ?> that = (SynsetNode<?, ?>) o;
        return Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }

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
        return "{id=" + getId() + ": " + getWords() + "}";
    }

    public enum POS {
        NOUN, VERB, ADJECTIVE, ADVERB;
    }
}
