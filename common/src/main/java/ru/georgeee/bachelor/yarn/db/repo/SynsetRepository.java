package ru.georgeee.bachelor.yarn.db.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.georgeee.bachelor.yarn.db.entity.Synset;

public interface SynsetRepository extends JpaRepository<Synset, Integer> {
    Synset findByExternalId(String externalId);
}
