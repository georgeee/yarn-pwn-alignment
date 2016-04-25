package ru.georgeee.bachelor.yarn.core;

import lombok.Getter;
import lombok.Setter;
import ru.georgeee.bachelor.yarn.dict.Dict;

import java.util.Collections;

@Getter
public class TranslationLink {
    private final String word;
    private final Dict.Translation translation;
    @Setter
    private double weight;

    public TranslationLink(String word, Dict.Translation translation, double weight) {
        this.word = word;
        this.translation = translation;
        this.weight = weight;
    }

    public TranslationLink(double weight) {
        this(null, new Dict.Translation(Collections.emptyList()), weight);
    }
}
