package ru.georgeee.bachelor.yarn.dict;

import ru.georgeee.bachelor.yarn.dict.Dict;
import ru.georgeee.bachelor.yarn.core.POS;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StatTrackingDict implements Dict {
    private final Dict dict;
    private final Set<String> notTranslated = new HashSet<>();
    private final Set<String> badTranslated = new HashSet<>();
    private final Set<String> goodTranslated = new HashSet<>();

    public StatTrackingDict(Dict dict) {
        this.dict = dict;
    }

    @Override
    public List<Translation> translate(String word) {
        List<Translation> translations = dict.translate(word);
        if (translations.isEmpty()) {
            notTranslated.add(word);
        }
        return translations;
    }

    public void reportResult(String word, boolean good) {
        if (!notTranslated.contains(word)) {
            if (good) {
                badTranslated.remove(word);
                goodTranslated.add(word);
            } else if (!goodTranslated.contains(word)) {
                badTranslated.add(word);
            }
        }
    }

    public void printStats(Appendable out) throws IOException {
        int total = notTranslated.size() + badTranslated.size() + goodTranslated.size();
        out.append(String.format("Not translated: %d / %d words\n", notTranslated.size(), total));
        out.append(String.format("Bad translated: %d / %d words\n", badTranslated.size(), total));
        out.append(String.format("Good translated: %d / %d words\n", goodTranslated.size(), total));
        out.append("Not translated:\n");
        for (String word : notTranslated) {
            out.append("   ").append(word).append('\n');
        }
        out.append("Bad translated:\n");
        for (String word : badTranslated) {
            out.append("   ").append(word).append('\n');
        }
    }
}
