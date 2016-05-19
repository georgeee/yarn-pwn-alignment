package ru.georgeee.bachelor.yarn.db.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.georgeee.bachelor.yarn.db.entity.DictCacheEntry;

public interface DictCacheRepository extends JpaRepository<DictCacheEntry, Integer> {
    DictCacheEntry findByDictKeyAndWord(String dictKey, String word);
}
