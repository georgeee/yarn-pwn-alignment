package ru.georgeee.bachelor.yarn.dict.wikt;

import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerFactory;
import ru.georgeee.bachelor.yarn.dict.Dict;

import java.util.Collections;
import java.util.List;

public class DictImpl implements Dict {
    private static final Logger log = Logger.getLogger(DictImpl.class);

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
