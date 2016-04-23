package ru.georgeee.bachelor.yarn.graph;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

@Getter
public class TranslationLink {
    private final String word;
    private final List<String> translationMeaning;
    @Setter
    private double weight;

    public TranslationLink(String word, List<String> translationMeaning, double weight) {
        this.word = word;
        this.translationMeaning = translationMeaning;
        this.weight = weight;
    }

    public TranslationLink(double weight) {
        this(null, Collections.emptyList(), weight);
    }
}
