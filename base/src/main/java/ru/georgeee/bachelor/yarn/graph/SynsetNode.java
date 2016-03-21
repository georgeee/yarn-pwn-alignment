package ru.georgeee.bachelor.yarn.graph;

import lombok.Getter;

import java.util.*;

@Getter
public abstract class SynsetNode<T, V> {
    private final Map<SynsetNode<V, T>, TranslationLink> edges = new HashMap<>();
    private final T data;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SynsetNode<?, ?> that = (SynsetNode<?, ?>) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    protected SynsetNode(T data) {
        this.data = data;
    }

    public abstract String getId();

    public abstract Set<String> getWords();

    public abstract POS getPOS();

    @Override
    public String toString() {
        return "{id=" + getId() + ": " + getWords() + "}";
    }

    public enum POS {
        NOUN, VERB, ADJECTIVE, ADVERB;
    }
}
