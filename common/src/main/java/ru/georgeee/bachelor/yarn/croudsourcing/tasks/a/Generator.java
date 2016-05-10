package ru.georgeee.bachelor.yarn.croudsourcing.tasks.a;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.ISynset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;
import ru.georgeee.bachelor.yarn.Yarn;
import ru.georgeee.bachelor.yarn.app.PWNNodeRepository;
import ru.georgeee.bachelor.yarn.app.YarnNodeRepository;
import ru.georgeee.bachelor.yarn.core.SynsetNode;
import ru.georgeee.bachelor.yarn.db.entity.PwnSynset;
import ru.georgeee.bachelor.yarn.db.entity.Synset;
import ru.georgeee.bachelor.yarn.db.entity.TranslateEdge;
import ru.georgeee.bachelor.yarn.db.entity.YarnSynset;
import ru.georgeee.bachelor.yarn.db.entity.tasks.a.Pool;
import ru.georgeee.bachelor.yarn.db.entity.tasks.a.Task;
import ru.georgeee.bachelor.yarn.db.entity.tasks.a.TaskSynset;
import ru.georgeee.bachelor.yarn.db.repo.SynsetRepository;
import ru.georgeee.bachelor.yarn.db.repo.tasks.a.PoolRepository;
import ru.georgeee.bachelor.yarn.xml.SynsetEntry;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class Generator {
    private static final String INPUT_TSV_FILENAME = "input.tsv";
    private static final String INPUT_JSON_FILENAME = "input.json";
    private static final Gson gson = new GsonBuilder().create();
    private static final Logger log = LoggerFactory.getLogger(Generator.class);
    @Autowired
    private PoolRepository poolRepository;
    @Autowired
    private Settings settings;
    @Autowired
    private Yarn yarn;
    @Autowired
    private IDictionary pwnDict;
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
                TsvSettings tsvSettings = createTsvSettings(task);
                out.write(task.getPwnSynset().getExternalId(), task.getId(), gson.toJson(tsvSettings));
                TaskConfig taskConfig = new TaskConfig();
                taskConfig.pwnId = task.getPwnSynset().getExternalId();
                taskConfig.taskId = task.getId();
                taskConfig.settings = tsvSettings;
                taskConfigs.add(taskConfig);
            }
        }
        log.info("Generated TSV file {}", tsvPath);
        Path jsonPath = poolDir.resolve(INPUT_JSON_FILENAME);
        try(BufferedWriter bw = Files.newBufferedWriter(jsonPath)){
            gson.toJson(taskConfigs, bw);
        }
        log.info("Generated JSON file {}", jsonPath);
        return tsvPath;
    }

    private TsvSettings createTsvSettings(Task task) {
        PwnSynset pwnSynset = task.getPwnSynset();
        PWNNodeRepository<?> pwnRepo = new PWNNodeRepository<>(pwnDict);
        YarnNodeRepository<?> yarnRepo = new YarnNodeRepository<>(yarn);
        TsvSettings res = new TsvSettings();
        SynsetNode<ISynset, ?> pwnNode = pwnRepo.getNodeById(pwnSynset.getExternalId());
        if (pwnNode == null) {
            throw new IllegalStateException("Pwn node not found by id " + pwnSynset.getExternalId());
        }
        res.pwn = new TsvSettings.Pwn();
        res.pwn.gloss = pwnNode.getGloss();
        res.pwn.examples = pwnNode.getExamples();
        res.pwn.words = pwnNode.getWords();
        res.options = new ArrayList<>();
        for (TaskSynset ts : task.getTaskSynsets()) {
            YarnSynset yarnSynset = ts.getYarnSynset();
            SynsetNode<SynsetEntry, ?> yarnNode = yarnRepo.getNodeById(yarnSynset.getExternalId());
            if (yarnNode == null) {
                throw new IllegalStateException("Pwn node not found by id " + yarnSynset.getExternalId());
            }
            TsvSettings.Option option = new TsvSettings.Option();
            option.id = ts.getId();
            option.words = yarnNode.getWords();
            res.options.add(option);
        }
        int imageCnt = Math.min(pwnSynset.getImages().size(), settings.getMaxImages());
        res.pwn.images = new ArrayList<>();
        for (int i = 0; i < imageCnt; ++i) {
            res.pwn.images.add(pwnSynset.getImages().get(i).getFilename());
        }
        return res;
    }

    @Transactional
    public void generateAndExportByIds(List<String> ids) throws IOException {
        Pool pool = generateByPwnIds(ids, null);
        log.info("Created pool #{}", pool.getId());
        exportToTsv(pool);
    }

    private static class TaskConfig {
        TsvSettings settings;
        String pwnId;
        int taskId;
    }

    private static class TsvSettings {
        Pwn pwn;
        List<Option> options;

        static class Pwn {
            String gloss;
            Collection<String> examples;
            Collection<String> words;
            List<String> images;
        }

        static class Option {
            Collection<String> words;
            int id;
        }
    }
}
