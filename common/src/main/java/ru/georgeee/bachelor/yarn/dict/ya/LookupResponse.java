package ru.georgeee.bachelor.yarn.dict.ya;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LookupResponse {
    private List<Def> def;

    @Getter
    @Setter
    public static class Def {
        private String text;
        private String pos;
        @SerializedName("ts")
        private String transcription;
        @SerializedName("tr")
        private List<Translation> translations;
    }

    @Getter
    @Setter
    public static class Translation {
        private String text;
        private String pos;
        @SerializedName("anm")
        private String animality;
        @SerializedName("gen")
        private String gender;
        @SerializedName("syn")
        private List<Word> synonyms;
        @SerializedName("mean")
        private List<Word> meanings;
        @SerializedName("ex")
        private List<TWord> examples;
    }

    @Getter
    @Setter
    public static class TWord {
        private String text;
        private String pos;
        private String gen;
        @SerializedName("tr")
        private List<Word> translations;
    }

    @Getter
    @Setter
    public static class Word {
        private String text;
        private String pos;
        private String gen;
    }


}
