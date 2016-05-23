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
import ru.georgeee.bachelor.yarn.db.entity.YarnSynset;
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
import java.util.stream.Collectors;

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

    private Set<YarnSynset> getBaseCandidates(PwnSynset pwnSynset) {
        List<TranslateEdge> allNMEdges = pwnSynset.getNotMasteredTranslateEdges();
        int totalCandidates = Math.min(allNMEdges.size(), settings.getNMax());
        if (totalCandidates == 0) return null;
        return allNMEdges.subList(0, totalCandidates).stream()
                .map(TranslateEdge::getYarnSynset)
                .collect(Collectors.toSet());
    }

    private void filterEdgesFromPredecessors(Pool pred, Map<PwnSynset, Set<YarnSynset>> map, Map<PwnSynset, Map<YarnSynset, List<Task>>> sources) {
        for (Task task : pred.getTasks()) {
            Set<YarnSynset> candidates = map.get(task.getPwnSynset());
            if (candidates == null) continue;
            Set<Integer> winners = computeWinners(task);
            for (TaskSynset ts : task.getTaskSynsets()) {
                YarnSynset yarnSynset = ts.getYarnSynset();
                if (winners == null || winners.contains(ts.getId())) {
                    sources
                            .computeIfAbsent(task.getPwnSynset(), _k -> new HashMap<>())
                            .computeIfAbsent(yarnSynset, _k -> new ArrayList<>())
                            .add(task);
                } else {
                    candidates.remove(yarnSynset);
                }
            }
        }
    }

    private Pool generate(List<PwnSynset> synsets, List<Integer> predecessorIds) {
        Pool pool = new Pool();
        List<Pool> predecessors = poolRepository.findAll(predecessorIds);
        pool.setPredecessors(predecessors);
        Map<PwnSynset, Map<YarnSynset, List<Task>>> allSources = new HashMap<>();
        Map<PwnSynset, Set<YarnSynset>> map = new HashMap<>();
        for (PwnSynset pwnSynset : synsets) {
            Set<YarnSynset> candidates = getBaseCandidates(pwnSynset);
            if (candidates != null)
                map.put(pwnSynset, candidates);
        }
        for (Pool pred : predecessors) {
            filterEdgesFromPredecessors(pred, map, allSources);
        }
        for (PwnSynset pwnSynset : map.keySet()) {
            Set<YarnSynset> candidates = map.get(pwnSynset);
            Map<YarnSynset, List<Task>> sources = allSources.get(pwnSynset);
            if (!candidates.isEmpty()) {
                pool.getTasks().addAll(composeTasks(pool, pwnSynset, candidates, sources));
            }
        }
        poolRepository.save(pool);
        return pool;
    }

    private List<Task> composeTasks(Pool pool, PwnSynset pwnSynset, Set<YarnSynset> candidates, Map<YarnSynset, List<Task>> sources) {
        int totalCandidates = candidates.size();
        int taskCount = totalCandidates / settings.getDMax() + (totalCandidates % settings.getDMax() == 0 ? 0 : 1);
        log.info("Generate: pwn={} total={} taskCount={}", pwnSynset.getExternalId(), totalCandidates, taskCount);
        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < taskCount; ++i) {
            Task task = new Task();
            task.setPool(pool);
            task.setPwnSynset(pwnSynset);
            tasks.add(task);
        }
        int i = 0;
        for (YarnSynset yarnSynset : candidates) {
            Task task = tasks.get(i++ % taskCount);
            TaskSynset taskSynset = new TaskSynset();
            taskSynset.setTask(task);
            taskSynset.setYarnSynset(yarnSynset);
            task.getTaskSynsets().add(taskSynset);
        }

        return sources == null ? tasks : tasks.stream().filter(task -> {
            Set<Task> predTasks = new HashSet<>();
            for (TaskSynset ts : task.getTaskSynsets()) {
                List<Task> l = sources.get(ts.getYarnSynset());
                if (l == null) {
                    predTasks.add(null);
                } else {
                    predTasks.addAll(l);
                }
            }
            return predTasks.contains(null) || predTasks.size() > 1;
        }).collect(Collectors.toList());
    }

    private Set<Integer> computeWinners(Task task) {
        Set<Integer> winners = new HashSet<>();
        boolean hasAnyAggr = false;
        for (Aggregation aggr : task.getAggregations()) {
            if (!settings.getAggrTags().contains(aggr.getTag()))
                continue;
            hasAnyAggr = true;
            if (aggr.getWeight() < settings.getAggrThreshold())
                continue;
            winners.add(aggr.getSelectedId());
        }
        return hasAnyAggr ? winners : null;
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
    public void generateAndExportByIds(List<String> ids, List<Integer> predecessorIds) throws IOException {
        List<PwnSynset> pwnSynsets = new ArrayList<>();
        for (String id : ids) {
            Synset s = synsetRepository.findByExternalId(id);
            if (s instanceof PwnSynset) {
                pwnSynsets.add((PwnSynset) s);
            }
        }
        Pool pool = generate(pwnSynsets, predecessorIds);
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
