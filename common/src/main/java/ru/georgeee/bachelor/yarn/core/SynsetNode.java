package ru.georgeee.bachelor.yarn.core;

import lombok.Getter;

import java.util.*;

public abstract class SynsetNode<T, V> {
    @Getter
    private final Map<SynsetNode<V, T>, TranslationLink> edges = new HashMap<>();
    @Getter
    private double maxBackEdgeWeight;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SynsetNode<?, ?> that = (SynsetNode<?, ?>) o;
        return Objects.equals(getId(), that.getId());
    }

    public void reportBackEdgeWeight(double weight) {
        if (maxBackEdgeWeight < weight) {
            maxBackEdgeWeight = weight;
        }
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    public abstract String getGloss();

    public List<String> getGlosses() {
        String gloss = getGloss();
        return gloss == null ? Collections.emptyList() : Collections.singletonList(gloss);
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
