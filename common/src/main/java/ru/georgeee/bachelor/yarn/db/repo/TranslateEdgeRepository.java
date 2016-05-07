package ru.georgeee.bachelor.yarn.db.repo;

import ru.georgeee.bachelor.yarn.db.entity.PwnSynset;
import ru.georgeee.bachelor.yarn.db.entity.TranslateEdge;
import org.springframework.data.repository.CrudRepository;
import ru.georgeee.bachelor.yarn.db.entity.YarnSynset;

public interface TranslateEdgeRepository extends CrudRepository<TranslateEdge, Integer> {
    TranslateEdge findByPwnSynsetAndYarnSynset(PwnSynset pwnSynset, YarnSynset yarnSynset);
}
