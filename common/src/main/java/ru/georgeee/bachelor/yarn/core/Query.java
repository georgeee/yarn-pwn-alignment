package ru.georgeee.bachelor.yarn.core;

import lombok.Getter;

@Getter
public class Query {
    private final String word;
    private final POS pos;

    public Query(String word, POS pos) {
        this.word = word;
        this.pos = pos;
    }
}
