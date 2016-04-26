package ru.georgeee.bachelor.yarn.dict.wikt;

import ru.georgeee.bachelor.yarn.dict.Dict;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class DictImpl implements Dict {
    private static final Logger log = LoggerFactory.getLogger(DictImpl.class);

    private final WiktionaryDictEngine dict;
    private final WiktLang from;
    private final WiktLang to;

    public DictImpl(WiktionaryDictEngine dict, WiktLang from, WiktLang to) {
        this.dict = dict;
        this.from = from;
        this.to = to;
    }

    @Override
    public List<Translation> translate(String word) {
        try {
            return dict.lookup(word, from, to);
        } catch (Exception e) {
            log.warn("Error communicating to slovari.yandex", e);
            return Collections.emptyList();
        }
    }
}
