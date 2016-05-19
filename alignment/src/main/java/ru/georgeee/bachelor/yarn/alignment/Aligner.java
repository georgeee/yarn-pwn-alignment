package ru.georgeee.bachelor.yarn.alignment;

import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.ISynset;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.georgeee.bachelor.yarn.Yarn;
import ru.georgeee.bachelor.yarn.alignment.misc.StageList;
import ru.georgeee.bachelor.yarn.app.Metrics;
import ru.georgeee.bachelor.yarn.app.PWNNodeRepository;
import ru.georgeee.bachelor.yarn.app.YarnNodeRepository;
import ru.georgeee.bachelor.yarn.clustering.Clusterer;
import ru.georgeee.bachelor.yarn.core.GraphTraverser;
import ru.georgeee.bachelor.yarn.core.NodeRepository;
import ru.georgeee.bachelor.yarn.core.Query;
import ru.georgeee.bachelor.yarn.core.SynsetNode;
import ru.georgeee.bachelor.yarn.xml.SynsetEntry;

import java.io.IOException;
import java.util.*;

@Component
public class Aligner {
    private static final Logger log = LoggerFactory.getLogger(Aligner.class);
    @Autowired
    private Yarn yarn;
    @Autowired
    private IDictionary pwnDict;
    @Autowired
    private Metrics metrics;
    @Autowired
    private Clusterer clusterer;

    @Autowired
    @Qualifier("alignmentStages")
    private StageList stages;
    @Autowired
    private GraphDbService graphDbService;
    @Value("${gr.minEdges:1}")
    private int minEdges;

    public Set<String> alignAndExport(Collection<String> ids) throws IOException {
        Set<String> remained = new HashSet<>(ids);
        for(int i = 0; i < stages.size(); ++i){
            Stage stage = stages.get(i);
            Result<ISynset, SynsetEntry> res = align(stage, remained);
            for (SynsetNode<ISynset, SynsetEntry> pwnNode : res.getRepo().getNodes()) {
                if (res.getIgnored().contains(pwnNode)) {
                    continue;
                }
                int count = graphDbService.exportToDb(stage, pwnNode);
                if(count >= minEdges){
                    remained.remove(pwnNode.getId());
                }
            }
            if (remained.isEmpty()) break;
            log.info("Stage {}: remained={}", i, remained);
        }
        return remained;
    }

    public Result<ISynset, SynsetEntry> align(Stage stage, Collection<String> ids) throws IOException {
        NodeRepository<ISynset, SynsetEntry> repo = new PWNNodeRepository<>(pwnDict);
        NodeRepository<SynsetEntry, ISynset> revRepo = new YarnNodeRepository<>(yarn);
        return new Result<>(repo, revRepo, align(stage, ids, repo, revRepo));
    }

    private <T, V> Set<SynsetNode<?, ?>> align(Stage stage, Collection<String> ids, NodeRepository<T, V> repo, NodeRepository<V, T> revRepo) throws IOException {
        GraphTraverser traverser = new GraphTraverser();
        traverser.registerRepo(repo, (n, s) -> {
            metrics.processNode(s, stage.getDirectDict(), revRepo, n);
            return null;
        });
        traverser.registerRepo(revRepo, (n, s) -> {
            metrics.processNode(s, stage.getReverseDict(), repo, n);
            return null;
        });
        for (String s : ids) {
            findNode(revRepo, s);
            findNode(repo, s);
        }
        traverser.traverse(stage.getSettings());
        clusterer.clusterizeOutgoingEdges(repo);
        return traverser.getRemained();
    }

    private <T, V> List<SynsetNode<T, V>> findNode(NodeRepository<T, V> repo, String s) {
        SynsetNode<T, V> node = repo.getNodeById(s);
        if (node != null) {
            return Collections.singletonList(node);
        }
        return repo.findNode(new Query(s, null));
    }

    @Getter
    @Setter
    public static class Result<T, V> {
        private final NodeRepository<T, V> repo;
        private final NodeRepository<V, T> revRepo;
        private final Set<SynsetNode<?, ?>> ignored;

        public Result(NodeRepository<T, V> repo, NodeRepository<V, T> revRepo, Set<SynsetNode<?, ?>> ignored) {
            this.repo = repo;
            this.revRepo = revRepo;
            this.ignored = ignored;
        }
    }

}
