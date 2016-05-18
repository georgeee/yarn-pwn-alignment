package ru.georgeee.bachelor.yarn.croudsourcing.tasks.a;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;
import ru.georgeee.bachelor.yarn.db.entity.Synset;
import ru.georgeee.bachelor.yarn.db.entity.tasks.a.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class MTsar {
    private static final Logger log = LoggerFactory.getLogger(MTsar.class);
    private static final String MTSAR_TASK_TYPE_SINGLE = "single";
    private static final String MTSAR_ANSWER_TYPE_ANSWER = "answer";
    @Autowired
    private DbService dbService;
    @Autowired
    private Settings settings;

    public void exportToMTsar(int poolId) throws IOException {
        Path dir = Paths.get(settings.getDir());
        Path poolDir = dir.resolve(String.valueOf(poolId)).resolve("mtsar");
        Files.createDirectories(poolDir);
        String stage = "alignment-taskA-pool" + poolId;
        Path workersFile = poolDir.resolve("workers.csv");
        Path tasksFile = poolDir.resolve("tasks.csv");
        Path answersFile = poolDir.resolve("answers.csv");
        Pool pool = dbService.getPoolWithTasks(poolId);
        try (CsvListWriter tasksWriter = new CsvListWriter(Files.newBufferedWriter(tasksFile), CsvPreference.STANDARD_PREFERENCE);
             CsvListWriter answersWriter = new CsvListWriter(Files.newBufferedWriter(answersFile), CsvPreference.STANDARD_PREFERENCE);
             CsvListWriter workersWriter = new CsvListWriter(Files.newBufferedWriter(workersFile), CsvPreference.STANDARD_PREFERENCE)) {
            Set<Worker> workers = new HashSet<>();
            tasksWriter.writeHeader("id", "stage", "datetime", "tags", "type", "description", "answers");
            answersWriter.writeHeader("id", "stage", "datetime", "tags", "type", "task_id", "worker_id", "answers");
            workersWriter.writeHeader("id", "stage", "datetime", "tags");
            for (Task task : pool.getTasks()) {
                String answerOptions = task.getTaskSynsets().stream().map(ts -> ts.getId().toString()).collect(Collectors.joining("|"));
                StringBuilder sb = new StringBuilder("For given pwn synset ");
                appendSynset(sb, task.getPwnSynset());
                sb.append(" choose one of yarn synsets ");
                for (TaskSynset ts : task.getTaskSynsets()) {
                    appendSynset(sb, ts.getYarnSynset());
                    sb.append(", ");
                }
                tasksWriter.write(task.getId(), stage, "", "", MTSAR_TASK_TYPE_SINGLE, sb.toString(), answerOptions + "|0");
                for (Answer answer : task.getAnswers()) {
                    Integer tsId = answer.getSelectedId();
                    answersWriter.write(answer.getId(), stage, answer.getCreatedDate().getTime(), "", MTSAR_ANSWER_TYPE_ANSWER, answer.getTaskId(), answer.getWorker().getId(), tsId == null ? 0 : tsId);
                    workers.add(answer.getWorker());
                }
            }
            for (Worker worker : workers) {
                workersWriter.write(worker.getId(), stage, "", "");
            }
        }
    }

    private void appendSynset(StringBuilder sb, Synset synset) {
        sb.append(synset.getExternalId()).append(" (").append(synset.getId()).append(")");
    }
}
