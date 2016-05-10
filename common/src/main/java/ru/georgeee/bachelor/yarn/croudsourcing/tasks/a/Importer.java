package ru.georgeee.bachelor.yarn.croudsourcing.tasks.a;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.georgeee.bachelor.yarn.croudsourcing.tasks.ImportUtils;
import ru.georgeee.bachelor.yarn.db.entity.tasks.a.Result;
import ru.georgeee.bachelor.yarn.db.entity.tasks.a.Task;
import ru.georgeee.bachelor.yarn.db.entity.tasks.a.TaskSynset;
import ru.georgeee.bachelor.yarn.db.repo.tasks.a.ResultRepository;
import ru.georgeee.bachelor.yarn.db.repo.tasks.a.TaskRepository;
import ru.georgeee.bachelor.yarn.db.repo.tasks.a.TaskSynsetRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

@Component
public class Importer {
    private static final Logger log = LoggerFactory.getLogger(Importer.class);

    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private TaskSynsetRepository tsRepository;
    @Autowired
    private ResultRepository resultRepository;

    @Transactional
    public void importFromJson(Path jsonPath, Result.Source source, String worker) throws IOException {
        String assignmentId = UUID.randomUUID().toString();
        Map<Integer, JsonResult> results = ImportUtils.importFromJson(jsonPath, JsonResult.class);
        results.entrySet().forEach(e -> {
            Task task = taskRepository.findOne(e.getKey());
            if (task == null) {
                log.warn("Importing {}: task #{} not found", jsonPath, e.getKey());
                return;
            }
            Result result = new Result();
            Integer selectedId = e.getValue().selectedId;
            TaskSynset taskSynset = null;
            if (selectedId != null && selectedId != 0) {
                taskSynset = tsRepository.findOne(selectedId);
                if (taskSynset == null) {
                    log.warn("Importing {}: taskSynset #{} not found", jsonPath, selectedId);
                    return;
                }
            }
            result.setTaskSynset(taskSynset);
            result.setSource(source);
            result.setWorker(worker);
            result.setTask(task);
            result.setAssignmentId(assignmentId);
            resultRepository.save(result);
        });
    }

    private static class JsonResult {
        Integer selectedId;
    }
}
