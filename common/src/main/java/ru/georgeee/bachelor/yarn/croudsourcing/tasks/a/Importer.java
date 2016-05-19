package ru.georgeee.bachelor.yarn.croudsourcing.tasks.a;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.supercsv.io.CsvMapReader;
import org.supercsv.prefs.CsvPreference;
import ru.georgeee.bachelor.yarn.croudsourcing.tasks.TaskUtils;
import ru.georgeee.bachelor.yarn.db.entity.tasks.a.Answer;
import ru.georgeee.bachelor.yarn.db.entity.tasks.a.Worker;
import ru.georgeee.bachelor.yarn.db.repo.tasks.a.AnswerRepository;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component("taskA_Importer")
public class Importer {
    private static final Logger log = LoggerFactory.getLogger(Importer.class);
    private static final String TOLOKA_TSV_ASSIGNMENT_ID = "ASSIGNMENT:assignment_id";
    private static final ThreadLocal<SimpleDateFormat> TSV_DATE_FORMAT2 = ThreadLocal
            .withInitial(() -> new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"));
    private static final ThreadLocal<SimpleDateFormat> TSV_DATE_FORMAT = ThreadLocal
            .withInitial(() -> new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"));
    @Autowired
    private DbService dbService;
    @Autowired
    private AnswerRepository answerRepository;
    @Autowired
    private TransactionTemplate txTemplate;
    private ExecutorService executorService;

    @PostConstruct
    private void init() {
        executorService = Executors.newFixedThreadPool(2 * Math.max(1, Runtime.getRuntime().availableProcessors()));
    }

    public void importAnswersFromToloka(Path tsvPath) throws IOException {
        Collection<Answer> answers = new ArrayList<>();
        try (CsvMapReader reader = new CsvMapReader(Files.newBufferedReader(tsvPath), CsvPreference.TAB_PREFERENCE)) {
            String[] header = reader.getHeader(true);
            Map<String, String> row;
            List<Map<String, String>> rows = new ArrayList<>();
            while ((row = reader.read(header)) != null) {
                if (row.get(TOLOKA_TSV_ASSIGNMENT_ID) == null) {
                    rows.add(row);
                } else {
                    String assignmentId = row.get(TOLOKA_TSV_ASSIGNMENT_ID);
                    String workerId = row.get("ASSIGNMENT:worker_id");
                    Worker worker = dbService.getOrCreateWorker(Worker.Source.TOLOKA, workerId);
                    Date started = extractDateFromToloka(row.get("ASSIGNMENT:started"));
                    log.info("Processing assignment {} ({} answers)", row.get(TOLOKA_TSV_ASSIGNMENT_ID), rows.size());
                    for (Map<String, String> _row : rows) {
                        Map<String, String> _rowFinal = _row;
                        Answer answer = extractAnswerFromToloka(_rowFinal, assignmentId, worker, started);
                        if (answer != null) {
                            answers.add(answer);
                        }
                    }
                    rows.clear();
                }
            }
        }
        try {
            answerRepository.save(answers);
        } catch (Exception e) {
            log.error("Error saving answers", e);
        }
    }

    private Date extractDateFromToloka(String dateString) {
        try {
            return TSV_DATE_FORMAT.get().parse(dateString);
        } catch (ParseException e) {
            try {
                return TSV_DATE_FORMAT2.get().parse(dateString);
            } catch (ParseException e1) {
                log.warn("Error parsing date date {}", dateString, e1);
                return new Date();
            }
        }
    }

    private Answer extractAnswerFromToloka(Map<String, String> row, String assignmentId, Worker worker, Date started) {
        int taskId = Integer.parseInt(row.get("INPUT:taskId"));
        Integer selectedId = null;
        String selectedIdString = row.get("OUTPUT:selectedId");
        if (StringUtils.isNotEmpty(selectedIdString)) {
            selectedId = Integer.parseInt(selectedIdString);
        }
        Answer answer = new Answer();
        answer.setCreatedDate(started);
        answer.setAssignmentId(assignmentId);
        answer.setWorker(worker);
        answer.setTaskId(taskId);
        if (selectedId == null) {
            //Should not be
            log.warn("Answer {} discarded: selectedId == null", answer);
            return null;
        } else {
            if (selectedId != 0) {
                answer.setSelectedId(selectedId);
            }
        }
        return answer;
    }

    @Transactional
    public void importAnswersFromJson(Path jsonPath, Worker.Source source, String workerId) throws IOException {
        String assignmentId = UUID.randomUUID().toString();
        Worker worker = dbService.getOrCreateWorker(source, workerId);
        Map<Integer, JsonAnswer> answers = TaskUtils.importFromJson(jsonPath, JsonAnswer.class);
        answers.entrySet().forEach(e -> {
            Answer answer = new Answer();
            Integer selectedId = e.getValue().selectedId;
            if (selectedId != null && selectedId != 0) {
                answer.setSelectedId(selectedId);
            }
            answer.setCreatedDate(new Date());
            answer.setWorker(worker);
            answer.setTaskId(e.getKey());
            answer.setAssignmentId(assignmentId);
            answerRepository.save(answer);
        });
    }

    private static class JsonAnswer {
        Integer selectedId;
    }
}
