package ru.georgeee.bachelor.yarn.core;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

public abstract class SynsetNode<T, V> {
    @Getter
    private final Map<SynsetNode<V, T>, TranslationLink> edges = new HashMap<>();
    @Getter @Setter
    private double maxBackEdgeWeight;
    @Getter @Setter
    private int backEdgeCount;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SynsetNode<?, ?> that = (SynsetNode<?, ?>) o;
        return Objects.equals(getId(), that.getId());
    }

    public void reportBackEdge(double weight) {
        if (maxBackEdgeWeight < weight) {
            maxBackEdgeWeight = weight;
        }
        backEdgeCount++;
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    public String getGloss() {
        return null;
    }

    public List<String> getExamples(){
        return Collections.emptyList();
    }

    public abstract String getId();

    public abstract Set<String> getWords();

    public abstract List<WordData> getWordsWithData();

    public abstract POS getPOS();

    @Override
    public String toString() {
        return "{id=" + getId() + ": " + getWordsWithData() + " (" + getGloss() + ")}";
    }

}
