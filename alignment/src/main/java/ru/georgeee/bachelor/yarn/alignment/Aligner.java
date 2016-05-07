package ru.georgeee.bachelor.yarn.alignment;

import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.ISynset;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.georgeee.bachelor.yarn.Yarn;
import ru.georgeee.bachelor.yarn.app.Metrics;
import ru.georgeee.bachelor.yarn.app.PWNNodeRepository;
import ru.georgeee.bachelor.yarn.app.YarnNodeRepository;
import ru.georgeee.bachelor.yarn.clustering.Clusterer;
import ru.georgeee.bachelor.yarn.core.*;
import ru.georgeee.bachelor.yarn.dict.Dict;
import ru.georgeee.bachelor.yarn.xml.SynsetEntry;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Component
public class Aligner {
    @Autowired
    private Yarn yarn;
    @Autowired
    private IDictionary pwnDict;
    @Autowired
    @Qualifier("enRuDict")
    private Dict enRuDict;
    @Autowired
    @Qualifier("ruEnDict")
    private Dict ruEnDict;
    @Autowired
    private Metrics metrics;

    @Autowired
    private GraphSettings grSettings;

    @Autowired
    private Clusterer clusterer;

    public Result align(List<String> ids) throws IOException {
        PWNNodeRepository<SynsetEntry> pwnRepo = new PWNNodeRepository<>(pwnDict);
        YarnNodeRepository<ISynset> yarnRepo = new YarnNodeRepository<>(yarn);
        GraphTraverser traverser = new GraphTraverser();
        traverser.registerRepo(pwnRepo, (n, s) -> {
            metrics.processNode(s, enRuDict, yarnRepo, n);
            return null;
        });
        traverser.registerRepo(yarnRepo, (n, s) -> {
            metrics.processNode(s, ruEnDict, pwnRepo, n);
            return null;
        });
        for (String s : ids) {
            findNode(yarnRepo, s);
            findNode(pwnRepo, s);
        }
        traverser.traverse(grSettings);
        clusterer.clusterizeOutgoingEdges(pwnRepo);
        return new Result(pwnRepo, yarnRepo, traverser.getRemained());
    }

    @Getter
    @Setter
    public static class Result {
        private final PWNNodeRepository<SynsetEntry> pwnRepo;
        private final YarnNodeRepository<ISynset> yarnRepo;
        private final Set<SynsetNode<?, ?>> ignored;

        public Result(PWNNodeRepository<SynsetEntry> pwnRepo, YarnNodeRepository<ISynset> yarnRepo, Set<SynsetNode<?, ?>> ignored) {
            this.pwnRepo = pwnRepo;
            this.yarnRepo = yarnRepo;
            this.ignored = ignored;
        }
    }

    private <T, V> List<SynsetNode<T, V>> findNode(NodeRepository<T, V> repo, String s) {
        SynsetNode<T, V> node = repo.getNodeById(s);
        if (node != null) {
            return Collections.singletonList(node);
        }
        return repo.findNode(new Query(s, null));
    }

}
