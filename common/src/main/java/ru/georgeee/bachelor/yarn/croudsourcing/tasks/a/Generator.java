package ru.georgeee.bachelor.yarn.croudsourcing.tasks.a;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;
import ru.georgeee.bachelor.yarn.croudsourcing.tasks.PwnOptionMapping;
import ru.georgeee.bachelor.yarn.croudsourcing.tasks.TaskUtils;
import ru.georgeee.bachelor.yarn.db.entity.PwnSynset;
import ru.georgeee.bachelor.yarn.db.entity.Synset;
import ru.georgeee.bachelor.yarn.db.entity.TranslateEdge;
import ru.georgeee.bachelor.yarn.db.entity.tasks.a.Aggregation;
import ru.georgeee.bachelor.yarn.db.entity.tasks.a.Pool;
import ru.georgeee.bachelor.yarn.db.entity.tasks.a.Task;
import ru.georgeee.bachelor.yarn.db.entity.tasks.a.TaskSynset;
import ru.georgeee.bachelor.yarn.db.repo.SynsetRepository;
import ru.georgeee.bachelor.yarn.db.repo.tasks.a.PoolRepository;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Component("taskA_Generator")
public class Generator {
    private static final String EXPORT_AGGREGATIONS_JSON = "aggregations.json";
    private static final String INPUT_TSV_FILENAME = "input.tsv";
    private static final String INPUT_JSON_FILENAME = "input.json";
    private static final Gson gson = new GsonBuilder().create();
    private static final Logger log = LoggerFactory.getLogger(Generator.class);
    @Autowired
    private PoolRepository poolRepository;
    @Autowired
    private Settings settings;
    @Autowired
    private TaskUtils taskUtils;
    @Autowired
    private SynsetRepository synsetRepository;

    @Transactional
    public Pool generateByPwnIds(List<String> ids, Pool predecessor) {
        List<PwnSynset> pwnSynsets = new ArrayList<>();
        for (String id : ids) {
            Synset s = synsetRepository.findByExternalId(id);
            if (s instanceof PwnSynset) {
                pwnSynsets.add((PwnSynset) s);
            }
        }
        return generate(pwnSynsets, predecessor);
    }

    public Pool generate(List<PwnSynset> synsets, Pool predecessor) {
        Pool pool = new Pool();
        pool.setPredecessor(predecessor);

        for (PwnSynset pwnSynset : synsets) {
            List<TranslateEdge> edges = pwnSynset.getNotMasteredTranslateEdges();
            List<Task> tasks = new ArrayList<>();
            int totalCandidates = Math.min(edges.size(), settings.getNMax());
            if (totalCandidates == 0) continue;
            int taskCount = totalCandidates / settings.getDMax() + (totalCandidates % settings.getDMax() == 0 ? 0 : 1);
            log.info("Generate: pwn={} total={} taskCount={}", pwnSynset.getExternalId(), totalCandidates, taskCount);
            for (int i = 0; i < taskCount; ++i) {
                Task task = new Task();
                task.setPool(pool);
                task.setPwnSynset(pwnSynset);
                tasks.add(task);
            }
            for (int i = 0; i < totalCandidates; ++i) {
                TranslateEdge edge = edges.get(i);
                Task task = tasks.get(i % taskCount);
                TaskSynset taskSynset = new TaskSynset();
                taskSynset.setTask(task);
                taskSynset.setYarnSynset(edge.getYarnSynset());
                task.getTaskSynsets().add(taskSynset);
            }
            pool.getTasks().addAll(tasks);
        }
        poolRepository.save(pool);
        return pool;
    }

    @Transactional
    public Path exportToTsv(Pool pool) throws IOException {
        Path dir = Paths.get(settings.getDir());
        Path poolDir = dir.resolve(String.valueOf(pool.getId()));
        Files.createDirectories(poolDir);
        Path tsvPath = poolDir.resolve(INPUT_TSV_FILENAME);
        List<TaskConfig> taskConfigs = new ArrayList<>();
        try (CsvListWriter out = new CsvListWriter(Files.newBufferedWriter(tsvPath), CsvPreference.TAB_PREFERENCE)) {
            out.writeHeader("INPUT:pwnId", "INPUT:taskId", "INPUT:settings");
            for (Task task : pool.getTasks()) {
                PwnOptionMapping optionMapping = taskUtils.createPwnOptionMapping(task.getPwnSynset(),
                        task.getTaskSynsets().stream().map(ts -> new ImmutablePair<>(ts.getId(), ts.getYarnSynset())),
                        settings.getMaxImages());
                out.write(task.getPwnSynset().getExternalId(), task.getId(), gson.toJson(optionMapping));
                TaskConfig taskConfig = new TaskConfig();
                taskConfig.pwnId = task.getPwnSynset().getExternalId();
                taskConfig.taskId = task.getId();
                taskConfig.settings = optionMapping;
                taskConfigs.add(taskConfig);
            }
        }
        log.info("Generated TSV file {}", tsvPath);
        Path jsonPath = poolDir.resolve(INPUT_JSON_FILENAME);
        try (BufferedWriter bw = Files.newBufferedWriter(jsonPath)) {
            gson.toJson(taskConfigs, bw);
        }
        log.info("Generated JSON file {}", jsonPath);
        return tsvPath;
    }

    @Transactional
    public void generateAndExportByIds(List<String> ids) throws IOException {
        Pool pool = generateByPwnIds(ids, null);
        log.info("Created pool #{}", pool.getId());
        exportToTsv(pool);
    }

    @Transactional(readOnly = true)
    public void exportAggregationsToJson(int poolId) throws IOException {
        Pool pool = Objects.requireNonNull(poolRepository.getOne(poolId));
        Map<String, Map<Integer, Map<Integer, Double>>> map = new HashMap<>();
        for (Task task : pool.getTasks()) {
            for (Aggregation aggr : task.getAggregations()) {
                map.computeIfAbsent(aggr.getTag(), _k -> new HashMap<>())
                        .computeIfAbsent(aggr.getTaskId(), _k -> new HashMap())
                        .put(aggr.getSelectedId() == null ? 0 : aggr.getSelectedId(), aggr.getWeight());
            }
        }
        Path dir = Paths.get(settings.getDir());
        Path poolDir = dir.resolve(String.valueOf(poolId));
        Files.createDirectories(poolDir);
        Path path = poolDir.resolve(EXPORT_AGGREGATIONS_JSON);
        try (BufferedWriter bw = Files.newBufferedWriter(path)) {
            gson.toJson(map, bw);
        }
    }

    private static class TaskConfig {
        PwnOptionMapping settings;
        String pwnId;
        int taskId;
    }

}
