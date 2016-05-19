package ru.georgeee.bachelor.yarn.croudsourcing.tasks.b;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.georgeee.bachelor.yarn.croudsourcing.tasks.PwnOptionMapping;
import ru.georgeee.bachelor.yarn.croudsourcing.tasks.TaskUtils;
import ru.georgeee.bachelor.yarn.db.entity.PwnSynset;
import ru.georgeee.bachelor.yarn.db.entity.Synset;
import ru.georgeee.bachelor.yarn.db.entity.TranslateEdge;
import ru.georgeee.bachelor.yarn.db.entity.tasks.b.Task;
import ru.georgeee.bachelor.yarn.db.repo.SynsetRepository;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Component("taskB_Generator")
public class Generator {
    private static final String INPUT_JSON_FILENAME = "input.json";
    private static final Gson gson = new GsonBuilder().create();
    private static final Logger log = LoggerFactory.getLogger(Generator.class);
    @Autowired
    private Settings settings;
    @Autowired
    private SynsetRepository synsetRepository;
    @Autowired
    private TaskUtils taskUtils;

    @Transactional
    public List<Task> generateByPwnIds(List<String> ids) {
        List<PwnSynset> pwnSynsets = new ArrayList<>();
        for (String id : ids) {
            Synset s = synsetRepository.findByExternalId(id);
            if (s instanceof PwnSynset) {
                pwnSynsets.add((PwnSynset) s);
            }
        }
        return generate(pwnSynsets);
    }

    @Transactional
    public void generateAndExportByIds(List<String> ids) throws IOException {
        List<Task> tasks = generateByPwnIds(ids);
        exportToTsv(tasks);
    }

    public List<Task> generate(List<PwnSynset> synsets) {
        List<Task> tasks = new ArrayList<>();
        for (PwnSynset pwnSynset : synsets) {
            List<TranslateEdge> edges = pwnSynset.getNotMasteredTranslateEdges();
            int totalCandidates = Math.min(edges.size(), settings.getNMax());
            if (totalCandidates == 0) continue;
            Task task = new Task();
            task.setPwnSynset(pwnSynset);
            for (int i = 0; i < totalCandidates; ++i) {
                TranslateEdge edge = edges.get(i);
                task.getYarnSynsets().add(edge.getYarnSynset());
            }
            tasks.add(task);
        }
        return tasks;
    }

    @Transactional
    public Path exportToTsv(List<Task> tasks) throws IOException {
        Path dir = Paths.get(settings.getDir());
        int lastId = Files.list(dir).map(Path::getFileName)
                .map(Path::toString).filter(StringUtils::isNumeric).map(Integer::parseInt)
                .max(Integer::compare).orElse(0);
        Path poolDir = dir.resolve(String.valueOf(lastId + 1));
        Files.createDirectories(poolDir);
        List<TaskConfig> taskConfigs = new ArrayList<>();
        for (Task task : tasks) {
            PwnOptionMapping optionMapping = taskUtils.createPwnOptionMapping(task.getPwnSynset(),
                    task.getYarnSynsets().stream().map(ys -> new ImmutablePair<>(ys.getId(), ys)),
                    settings.getMaxImages());
            TaskConfig taskConfig = new TaskConfig();
            taskConfig.taskId = task.getPwnSynset().getId();
            taskConfig.pwnId = task.getPwnSynset().getExternalId();
            taskConfig.settings = optionMapping;
            taskConfigs.add(taskConfig);
        }
        Path jsonPath = poolDir.resolve(INPUT_JSON_FILENAME);
        try (BufferedWriter bw = Files.newBufferedWriter(jsonPath)) {
            gson.toJson(taskConfigs, bw);
        }
        log.info("Generated JSON file {}", jsonPath);
        return jsonPath;
    }


    private static class TaskConfig {
        PwnOptionMapping settings;
        String pwnId;
        //internal pwnId
        int taskId;
    }

}
