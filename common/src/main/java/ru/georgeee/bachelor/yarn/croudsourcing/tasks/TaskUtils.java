package ru.georgeee.bachelor.yarn.croudsourcing.tasks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.ISynset;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.georgeee.bachelor.yarn.Yarn;
import ru.georgeee.bachelor.yarn.app.PWNNodeRepository;
import ru.georgeee.bachelor.yarn.app.YarnNodeRepository;
import ru.georgeee.bachelor.yarn.core.SynsetNode;
import ru.georgeee.bachelor.yarn.db.entity.PwnSynset;
import ru.georgeee.bachelor.yarn.db.entity.YarnSynset;
import ru.georgeee.bachelor.yarn.xml.SynsetEntry;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class TaskUtils {
    private static final Gson gson = new GsonBuilder().create();
    @Autowired
    private Yarn yarn;
    @Autowired
    private IDictionary pwnDict;

    public PwnOptionMapping createPwnOptionMapping(PwnSynset pwnSynset, Stream<Pair<Integer, YarnSynset>> ysStream, int maxImages) {
        PWNNodeRepository<?> pwnRepo = new PWNNodeRepository<>(pwnDict);
        YarnNodeRepository<?> yarnRepo = new YarnNodeRepository<>(yarn);
        PwnOptionMapping res = new PwnOptionMapping();
        SynsetNode<ISynset, ?> pwnNode = pwnRepo.getNodeById(pwnSynset.getExternalId());
        if (pwnNode == null) {
            throw new IllegalStateException("Pwn node not found by id " + pwnSynset.getExternalId());
        }
        res.pwn = new PwnOptionMapping.Pwn();
        res.pwn.gloss = pwnNode.getGloss();
        res.pwn.examples = pwnNode.getExamples();
        res.pwn.words = pwnNode.getWords();
        res.options = new ArrayList<>();
        ysStream.forEach(ysPair -> {
            YarnSynset yarnSynset = ysPair.getRight();
            SynsetNode<SynsetEntry, ?> yarnNode = yarnRepo.getNodeById(yarnSynset.getExternalId());
            if (yarnNode == null) {
                throw new IllegalStateException("Pwn node not found by id " + yarnSynset.getExternalId());
            }
            PwnOptionMapping.Option option = new PwnOptionMapping.Option();
            option.id = ysPair.getKey();
            option.words = yarnNode.getWords();
            res.options.add(option);
        });
        int imageCnt = Math.min(pwnSynset.getImages().size(), maxImages);
        res.pwn.images = new ArrayList<>();
        for (int i = 0; i < imageCnt; ++i) {
            res.pwn.images.add(pwnSynset.getImages().get(i).getFilename());
        }
        return res;
    }

    public static <T> Map<Integer, T> importFromJson(Path jsonPath, Class<? extends T> tClass) throws IOException {
        Map<Integer, T> results = new HashMap<>();
        try (BufferedReader br = Files.newBufferedReader(jsonPath)) {
            JsonObject jsonResults = gson.fromJson(br, JsonObject.class);
            jsonResults.entrySet().forEach(e -> {
                int taskId = Integer.parseInt(e.getKey());
                T jr = gson.fromJson(e.getValue(), tClass);
                results.put(taskId, jr);
            });
        }
        return results;
    }
}
