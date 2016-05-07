package ru.georgeee.bachelor.yarn.alignment;

import edu.mit.jwi.item.ISynset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import ru.georgeee.bachelor.yarn.clustering.Cluster;
import ru.georgeee.bachelor.yarn.core.GraphVizBuilder;
import ru.georgeee.bachelor.yarn.core.GraphVizSettings;
import ru.georgeee.bachelor.yarn.core.NodeRepository;
import ru.georgeee.bachelor.yarn.core.SynsetNode;
import ru.georgeee.bachelor.yarn.xml.SynsetEntry;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

@Service
public class ExportService {
    @Autowired
    private GraphVizSettings gvSettings;

    public void exportToGraphViz(Path graphvizOutFile, Aligner.Result result) throws IOException {
        exportToGraphViz(graphvizOutFile, result.getIgnored(), result.getPwnRepo(), result.getYarnRepo());
    }

    public void exportToGraphViz(Path graphvizOutFile, Collection<? extends SynsetNode<?, ?>> ignored, NodeRepository<?, ?>... repos) throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(graphvizOutFile);
             GraphVizBuilder builder = new GraphVizBuilder(gvSettings, bw)) {
            builder.addIgnored(ignored);
            for (NodeRepository<?, ?> repo : repos) {
                builder.addRepo(repo);
            }
            printNodes(builder.getCreated());
        }
    }

    private void unrollCluster(Cluster<?, ?> cluster, Set<SynsetNode<?, ?>> unrolled) {
        for (Cluster.Member<?, ?> n : cluster) {
            unrollNode(n.getNode(), unrolled);
        }
    }

    private void unrollNode(SynsetNode<?, ?> node, Set<SynsetNode<?, ?>> unrolled) {
        if (node instanceof Cluster) {
            unrollCluster((Cluster<?, ?>) node, unrolled);
        } else {
            unrolled.add(node);
        }
    }

    private void printNodes(Collection<? extends SynsetNode<?, ?>> created) {
        TreeSet<SynsetNode<?, ?>> unrolled = new TreeSet<>((s1, s2) -> s1.getId().compareTo(s2.getId()));
        created.stream().forEach(n -> unrollNode(n, unrolled));
        unrolled.stream().forEach(System.out::println);
    }


    @Autowired
    private ApplicationContext context;
    @Autowired
    private ExportServiceDB exportServiceDB;

    public void exportToDb(Aligner.Result result) {
        for (SynsetNode<ISynset, SynsetEntry> pwnNode : result.getPwnRepo().getNodes()) {
            if (result.getIgnored().contains(pwnNode)) {
                continue;
            }
            exportServiceDB.exportToDb(pwnNode);
        }
    }

}
