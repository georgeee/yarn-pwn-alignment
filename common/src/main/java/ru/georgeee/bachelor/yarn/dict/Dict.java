package ru.georgeee.bachelor.yarn.dict;

import lombok.Getter;
import lombok.ToString;
import ru.georgeee.bachelor.yarn.core.POS;

import java.util.List;

public interface Dict {
    List<Translation> translate(String word);

    @Getter @ToString
    class Translation {
        private final List<String> words;
        private final POS pos;
        private final String gloss;

        public Translation(List<String> words, POS pos, String gloss) {
            this.words = words;
            this.pos = pos;
            this.gloss = gloss;
        }

        public Translation(List<String> words) {
            this(words, null, null);
        }
    }
}
