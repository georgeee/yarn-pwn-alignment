package ru.georgeee.bachelor.yarn.graph;

import lombok.Getter;

import java.util.List;

@Getter
public class TranslationLink {
    private final String word;
    private final List<String> translationMeaning;
    private final double weight;

    public TranslationLink(String word, List<String> translationMeaning, double weight) {
        this.word = word;
        this.translationMeaning = translationMeaning;
        this.weight = weight;
    }
}
