package ru.georgeee.bachelor.yarn.croudsourcing.tasks.b;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.georgeee.bachelor.yarn.db.entity.tasks.b.AnswerSelected;
import ru.georgeee.bachelor.yarn.db.entity.tasks.b.BAnswer;
import ru.georgeee.bachelor.yarn.db.repo.tasks.b.BAnswerRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Component
public class Importer {
    private static final Gson gson = new GsonBuilder().create();
    private static final String JSON_CHECKED_PREFIX = "checked_";
    private static final String JSON_CLEAN_PREFIX = "clean_";
    private static final Logger log = LoggerFactory.getLogger(Importer.class);

    @Autowired
    private BAnswerRepository answerRepository;

    @Transactional
    public void importAnswersFromJson(Path path, String author) throws IOException {
        String assignmentId = UUID.randomUUID().toString();
        JsonAnswers answers;
        try (BufferedReader br = Files.newBufferedReader(path)) {
            answers = gson.fromJson(br, JsonAnswers.class);
        }
        log.debug("Importing {}", answers);
        List<BAnswer> answerEntities = new ArrayList<>();
        answers.entrySet().forEach(e -> {
            BAnswer answer = new BAnswer();
            Map<String, Integer> vals = e.getValue();
            if (vals.getOrDefault("none", 0) != 1) {
                for (Map.Entry<String, Integer> e2 : vals.entrySet()) {
                    String key = e2.getKey();
                    if (!key.startsWith(JSON_CHECKED_PREFIX) || e2.getValue() != 1)
                        continue;
                    int yarnId = Integer.parseInt(key.substring(JSON_CHECKED_PREFIX.length()));
                    boolean isClean = vals.getOrDefault(JSON_CLEAN_PREFIX + yarnId, 0) == 1;
                    AnswerSelected opt = new AnswerSelected();
                    opt.setAnswer(answer);
                    opt.getEmbeddedId().setYarnId(yarnId);
                    opt.setClean(isClean);
                    answer.getSelectedList().add(opt);
                }
            }
            answer.setCreatedDate(new Date());
            answer.setWorker(author);
            answer.setPwnId(e.getKey());
            answer.setAssignmentId(assignmentId);
            answerEntities.add(answer);
        });
        answerRepository.save(answerEntities);
    }

    private static class JsonAnswers extends HashMap<Integer, Map<String, Integer>> {
    }
}
