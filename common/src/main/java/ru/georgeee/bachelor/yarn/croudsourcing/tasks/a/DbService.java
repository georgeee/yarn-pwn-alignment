package ru.georgeee.bachelor.yarn.croudsourcing.tasks.a;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.georgeee.bachelor.yarn.db.entity.tasks.a.Pool;
import ru.georgeee.bachelor.yarn.db.entity.tasks.a.Task;
import ru.georgeee.bachelor.yarn.db.entity.tasks.a.Worker;
import ru.georgeee.bachelor.yarn.db.repo.tasks.a.PoolRepository;
import ru.georgeee.bachelor.yarn.db.repo.tasks.a.TaskRepository;
import ru.georgeee.bachelor.yarn.db.repo.tasks.a.TaskSynsetRepository;
import ru.georgeee.bachelor.yarn.db.repo.tasks.a.WorkerRepository;


@Component("taskA_DbService")
class DbService {

    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private TaskSynsetRepository tsRepository;
    @Autowired
    private PoolRepository poolRepository;
    @Autowired
    private WorkerRepository workerRepository;

    @Transactional
    @Cacheable("TaskA_Importer_Worker")
    public Worker getOrCreateWorker(Worker.Source source, String externalId) {
        Worker worker = workerRepository.findBySourceAndExternalId(source, externalId);
        if (worker == null) {
            worker = new Worker();
            worker.setExternalId(externalId);
            worker.setSource(source);
            workerRepository.save(worker);
        }
        Hibernate.initialize(worker.getId());
        return worker;
    }

    @Transactional(readOnly = true)
    public Pool getPoolWithTasks(int poolId) {
        Pool pool = poolRepository.getOne(poolId);
        for (Task task : pool.getTasks()) {
            Hibernate.initialize(task.getAnswers());
            Hibernate.initialize(task.getTaskSynsets());
        }
        return pool;
    }
}
