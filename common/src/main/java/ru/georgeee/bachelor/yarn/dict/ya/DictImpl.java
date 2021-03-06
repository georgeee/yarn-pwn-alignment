package ru.georgeee.bachelor.yarn.dict.ya;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.georgeee.bachelor.yarn.core.POS;
import ru.georgeee.bachelor.yarn.dict.Dict;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DictImpl implements Dict, Closeable {
    private static final Logger log = LoggerFactory.getLogger(DictImpl.class);
    private final YandexDictEngine yaDict;
    private final String dir;

    public DictImpl(YandexDictEngine yaDict, String dir) {
        this.yaDict = yaDict;
        this.dir = dir;
    }

    @Override
    public List<Translation> translate(String word) {
        try {
            LookupResponse response = yaDict.translate(word, dir);
            Stream<LookupResponse.Def> s = response.getDef().stream();
            return s.flatMap(def -> def.getTranslations()
                    .stream()
                    .map(tr -> {
                        Set<String> words = new LinkedHashSet<>();
                        words.add(tr.getText());
                        if (tr.getSynonyms() != null) {
                            tr.getSynonyms().stream().map(LookupResponse.Word::getText).forEach(words::add);
                        }
                        POS pos = YandexDictEngine.determinePOS(tr.getPos());
                        String gloss = null;
                        List<LookupResponse.Word> meanings = tr.getMeanings();
                        if (meanings != null && !meanings.isEmpty()) {
                            gloss = meanings.stream().map(LookupResponse.Word::getText).collect(Collectors.joining(", "));
                        }
                        return new Translation(words, pos, gloss);
                    }))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Error communicating to slovari.yandex", e);
            return Collections.emptyList();
        }
    }

    @Override
    public void close() throws IOException {
        yaDict.close();
    }
}
