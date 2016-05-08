package ru.georgeee.bachelor.yarn.croudsourcing.tasks.a;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.ISynset;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.georgeee.bachelor.yarn.Yarn;
import ru.georgeee.bachelor.yarn.app.PWNNodeRepository;
import ru.georgeee.bachelor.yarn.app.YarnNodeRepository;
import ru.georgeee.bachelor.yarn.core.SynsetNode;
import ru.georgeee.bachelor.yarn.db.entity.PwnSynset;
import ru.georgeee.bachelor.yarn.db.entity.TranslateEdge;
import ru.georgeee.bachelor.yarn.db.entity.YarnSynset;
import ru.georgeee.bachelor.yarn.db.entity.tasks.a.Pool;
import ru.georgeee.bachelor.yarn.db.entity.tasks.a.Task;
import ru.georgeee.bachelor.yarn.db.entity.tasks.a.TaskSynset;
import ru.georgeee.bachelor.yarn.db.repo.tasks.a.PoolRepository;
import ru.georgeee.bachelor.yarn.xml.SynsetEntry;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Component
public class Generator {
    private static final String INPUT_FILENAME = "input.tsv";
    private static final Gson gson = new GsonBuilder().create();
    @Autowired
    private PoolRepository poolRepository;
    @Autowired
    private Settings settings;
    @Autowired
    private Yarn yarn;
    @Autowired
    private IDictionary pwnDict;

    public Pool generate(List<PwnSynset> synsets, Pool predecessor) {
        Pool pool = new Pool();
        pool.setOverlap(settings.getDefaultOverlap());
        pool.setPredecessor(predecessor);
        pool.setStatus(Pool.Status.INCOMPLETE);

        for (PwnSynset pwnSynset : synsets) {
            List<TranslateEdge> edges = pwnSynset.getNotMasteredTranslateEdges();
            int totalCandidates = Math.max(edges.size(), settings.getNMax());
            int taskCount = totalCandidates / settings.getDMax() + (totalCandidates % settings.getDMax() > 0 ? 0 : 1);
            for (int i = 0; i < taskCount; ++i) {
                Task task = new Task();
                task.setPool(pool);
                task.setPwnSynset(pwnSynset);
                pool.getTasks().add(task);
            }
            for (int i = 0; i < totalCandidates; ++i) {
                TranslateEdge edge = edges.get(i);
                Task task = pool.getTasks().get(i % taskCount);
                TaskSynset taskSynset = new TaskSynset();
                taskSynset.setTask(task);
                taskSynset.setYarnSynset(edge.getYarnSynset());
                task.getTaskSynsets().add(taskSynset);
            }
        }
        poolRepository.save(pool);
        return pool;
    }

    public Path exportToTsv(Pool pool) throws IOException {
        Path dir = Paths.get(settings.getDir());
        Path poolDir = dir.resolve(String.valueOf(pool.getId()));
        Files.createDirectories(poolDir);
        Path tsvPath = poolDir.resolve(INPUT_FILENAME);
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(tsvPath))) {
            out.println("INPUT:pwnId\tINPUT:taskId\tINPUT:settings");
            for (Task task : pool.getTasks()) {
                out.printf("%s\t%d\t%s%n", task.getPwnSynset().getExternalId(), task.getId(), gson.toJson(createTsvSettings(task)));
            }
        }
        return tsvPath;
    }

    private TsvSettings createTsvSettings(Task task) {
        PWNNodeRepository<?> pwnRepo = new PWNNodeRepository<>(pwnDict);
        YarnNodeRepository<?> yarnRepo = new YarnNodeRepository<>(yarn);
        TsvSettings res = new TsvSettings();
        SynsetNode<ISynset, ?> pwnNode = pwnRepo.getNodeById(task.getPwnSynset().getExternalId());
        if (pwnNode == null) {
            throw new IllegalStateException("Pwn node not found by id " + task.getPwnSynset().getExternalId());
        }
        res.pwn = new TsvSettings.Pwn();
        res.pwn.gloss = pwnNode.getGloss();
        res.pwn.words = StringUtils.join(pwnNode.getWords(), ", ");
        res.options = new ArrayList<>();
        for (TaskSynset ts : task.getTaskSynsets()) {
            YarnSynset yarnSynset = ts.getYarnSynset();
            SynsetNode<SynsetEntry, ?> yarnNode = yarnRepo.getNodeById(yarnSynset.getExternalId());
            if (yarnNode == null) {
                throw new IllegalStateException("Pwn node not found by id " + yarnSynset.getExternalId());
            }
            TsvSettings.Option option = new TsvSettings.Option();
            option.id = ts.getId();
            option.words = StringUtils.join(yarnNode.getWords(), ", ");
            res.options.add(option);
        }
        return res;
    }

    private static class TsvSettings {
        Pwn pwn;
        List<Option> options;

        static class Pwn {
            String gloss;
            String words;
        }

        static class Option {
            String words;
            int id;
        }
    }
}
