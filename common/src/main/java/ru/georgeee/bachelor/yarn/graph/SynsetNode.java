package ru.georgeee.bachelor.yarn.graph;

import lombok.Getter;

import java.util.*;

@Getter
public abstract class SynsetNode<T, V> {
    private final Map<SynsetNode<V, T>, TranslationLink> edges = new HashMap<>();
    @Getter
    private double maxBackEdgeWeight;
    private final T data;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SynsetNode<?, ?> that = (SynsetNode<?, ?>) o;
        return Objects.equals(getId(), that.getId());
    }

    public void reportBackEdge(TranslationLink link) {
        if (maxBackEdgeWeight < link.getWeight()) {
            maxBackEdgeWeight = link.getWeight();
        }
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    protected SynsetNode(T data) {
        this.data = data;
    }

    public abstract String getGloss();

    public abstract String getId();

    public abstract Set<String> getWords();

    public abstract POS getPOS();

    @Override
    public String toString() {
        return "{id=" + getId() + ": " + getWords() + " (" + getGloss() + ")}";
    }

    public enum POS {
        NOUN, VERB, ADJECTIVE, ADVERB;
    }
}
