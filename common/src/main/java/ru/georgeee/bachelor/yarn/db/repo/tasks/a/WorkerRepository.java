package ru.georgeee.bachelor.yarn.db.repo.tasks.a;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.georgeee.bachelor.yarn.db.entity.tasks.a.Worker;

public interface WorkerRepository extends JpaRepository<Worker, Integer> {
    Worker findBySourceAndExternalId(Worker.Source source, String externalId);
}
