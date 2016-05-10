package ru.georgeee.bachelor.yarn.db.repo.tasks.a;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.georgeee.bachelor.yarn.db.entity.tasks.a.TaskSynset;

public interface TaskSynsetRepository extends JpaRepository<TaskSynset, Integer> {
}
