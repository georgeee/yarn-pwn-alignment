package ru.georgeee.bachelor.yarn.graph;

import lombok.Getter;

@Getter
public class Query {
    private final String word;
    private final SynsetNode.POS pos;

    public Query(String word, SynsetNode.POS pos) {
        this.word = word;
        this.pos = pos;
    }
}
