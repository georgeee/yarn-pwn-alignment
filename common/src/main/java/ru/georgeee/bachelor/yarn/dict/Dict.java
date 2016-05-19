package ru.georgeee.bachelor.yarn.dict;

import lombok.Getter;
import lombok.ToString;
import ru.georgeee.bachelor.yarn.core.POS;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

public interface Dict {
    List<Translation> translate(String word);

    @Getter @ToString
    class Translation implements Serializable {
        private static final long serialVersionUID = -1725106686462701575L;
        private final Set<String> words;
        private final POS pos;
        private final String gloss;

        public Translation(Set<String> words, POS pos, String gloss) {
            this.words = words;
            this.pos = pos;
            this.gloss = gloss;
        }

        public Translation(Set<String> words) {
            this(words, null, null);
        }
    }
}
