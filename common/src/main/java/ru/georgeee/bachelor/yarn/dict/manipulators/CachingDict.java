package ru.georgeee.bachelor.yarn.dict.manipulators;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.georgeee.bachelor.yarn.db.entity.DictCacheEntry;
import ru.georgeee.bachelor.yarn.db.repo.DictCacheRepository;
import ru.georgeee.bachelor.yarn.dict.Dict;

import java.util.Collections;
import java.util.List;

public class CachingDict implements Dict {
    private static final Logger log = LoggerFactory.getLogger(CachingDict.class);
    @Autowired
    private DictCacheRepository cacheRepo;

    @Getter @Setter
    private String dictKey;
    @Getter @Setter
    private Dict dict;
    @Getter @Setter
    private boolean cacheOnly;

    @Override
    public List<Translation> translate(String word) {
        DictCacheEntry entry = cacheRepo.findByDictKeyAndWord(dictKey, word);
        if (entry != null) {
            return entry.getTranslations();
        }
        if(cacheOnly){
            return Collections.emptyList();
        }
        List<Translation> translations = dict.translate(word);
        entry = new DictCacheEntry();
        entry.setDictKey(dictKey);
        entry.setWord(word);
        entry.setTranslations(translations);
        try {
            cacheRepo.save(entry);
        } catch (Exception e) {
            log.warn("Error saveing entry {}", e);
        }
        return translations;
    }
}
