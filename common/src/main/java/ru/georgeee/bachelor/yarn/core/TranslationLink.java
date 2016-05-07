package ru.georgeee.bachelor.yarn.core;

import lombok.Getter;
import lombok.Setter;
import ru.georgeee.bachelor.yarn.dict.Dict;

import java.util.Collections;

@Getter
public class TranslationLink {
    private final WordData wordData;
    private final Dict.Translation translation;
    @Setter
    private double weight;

    public TranslationLink(WordData wordData, Dict.Translation translation, double weight) {
        this.wordData = wordData;
        this.translation = translation;
        this.weight = weight;
    }

    public String getWord(){
        return wordData == null ? null : wordData.getLemma();
    }

    public TranslationLink(double weight) {
        this(null, new Dict.Translation(Collections.emptySet()), weight);
    }
}
