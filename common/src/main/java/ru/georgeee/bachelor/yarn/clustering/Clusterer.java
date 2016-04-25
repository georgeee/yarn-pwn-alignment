package ru.georgeee.bachelor.yarn.clustering;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.georgeee.bachelor.yarn.core.NodeRepository;
import ru.georgeee.bachelor.yarn.core.SynsetNode;
import ru.georgeee.bachelor.yarn.core.TranslationLink;

import java.util.*;

@Component
public class Clusterer {
    private static final Logger log = LoggerFactory.getLogger(Clusterer.class);
    @Value("${clustering.similarityThreshold:2}")
    private int similarityThreshold;

    public <T, V> void clusterizeOutgoingEdges(NodeRepository<T, V> repo) {
        for (SynsetNode<T, V> srcNode : repo.getNodes()) {
            PriorityQueue<Cluster.Member<V, T>> preClusters = new PriorityQueue<>();
            for (Map.Entry<SynsetNode<V, T>, TranslationLink> entry : srcNode.getEdges().entrySet()) {
                double weight = entry.getValue().getWeight();
                TranslationLink rLink = entry.getKey().getEdges().get(srcNode);
                double rWeight = rLink == null ? 0 : rLink.getWeight();
                preClusters.add(new Cluster.Member<>(entry.getKey(), weight, rWeight));
            }
            List<Cluster<V, T>> clusters = new ArrayList<>();
            while (!preClusters.isEmpty()) {
                Cluster.Member<V, T> preCluster = preClusters.remove();
                int i;
                for (i = 0; i < clusters.size(); ++i) {
                    Cluster<V, T> cluster = clusters.get(i);
                    if (cluster == null) continue;
                    if (testSimilar(preCluster, cluster)) {
                        break;
                    }
                }
                if (i < clusters.size()) {
                    Cluster<V, T> master = clusters.get(i++);
                    master.addMember(preCluster);
//                    log.debug("Precluster {}: master {}", preCluster.getNode().getId(), master.getId());
                    for (; i < clusters.size(); ++i) {
                        Cluster<V, T> cluster = clusters.get(i);
                        if (cluster == null) continue;
                        if (testSimilar(preCluster, cluster)) {
//                            log.debug("Merging {} into {}", cluster.getId(), master.getId());
                            for (Cluster.Member<V, T> member : cluster) {
                                master.addMember(member);
//                                log.debug(" member {}", member.getNode().getId());
                            }
                            clusters.set(i, null);
                        }
                    }
                } else {
//                    log.debug("Precluster {}: new cluster", preCluster.getNode().getId(), i, clusters.size());
                    clusters.add(new Cluster<>(srcNode, preCluster));
                }
            }
            for (Cluster<V, T> cluster : clusters) {
                if (cluster == null) continue;
                if (cluster.size() > 1) {
                    for (Cluster.Member<V, T> e : cluster) {
                        srcNode.getEdges().remove(e.getNode());
                        e.getNode().getEdges().remove(srcNode);
                    }
                    srcNode.getEdges().put(cluster, new TranslationLink(cluster.first().getWeight()));
                }
            }
        }
    }

    private <V, T> boolean testSimilar(Cluster.Member<V, T> preCluster, Cluster<V, T> cluster2) {
        for (Cluster.Member<V, T> e : cluster2) {
            if (testSimilar(preCluster.getNode(), e.getNode())) {
//                log.debug("testSimilar: {} {} -> {}", preCluster.getNode().getId(), cluster2.getId(), true);
                return true;
            }
        }
//        log.debug("testSimilar: {} {} -> {}", preCluster.getNode().getId(), cluster2.getId(), false);
        return false;
    }

    private <V, T> boolean testSimilar(SynsetNode<V, T> node1, SynsetNode<V, T> node2) {
        Set<String> words1 = new HashSet<>(node1.getWords());
        words1.retainAll(node2.getWords());
//        log.debug("testSimilar: {} {} -> {}", node1.getId(), node2.getId(), words1.size() >= similarityThreshold);
        return words1.size() >= similarityThreshold || (node1.getWords().size() == node2.getWords().size() && node1.getWords().size() == words1.size());
    }
}
