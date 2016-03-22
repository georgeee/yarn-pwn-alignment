package ru.georgeee.bachelor.yarn.alignment;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.georgeee.bachelor.yarn.graph.Query;
import ru.georgeee.bachelor.yarn.graph.SynsetNode;
import ru.georgeee.bachelor.yarn.graph.TranslationLink;
import ru.georgeee.bachelor.yarn.graph.NodeRepository;

import javax.annotation.PostConstruct;
import java.util.*;

@Component
public class Metrics {
    @Autowired
    private MetricsParams params;
    private NormalDistribution p1ND;
    private double p1C;

    @PostConstruct
    private void init() {
        p1ND = new NormalDistribution(null, 1 - params.getP1Mean(), params.getP1Sd());
        p1C = 1 / p1ND.density(params.getP1Mean());
    }

    public <T, V> void processNode(SimpleDict dict, NodeRepository<V, T> repo, SynsetNode<T, V> node) {
        Map<SynsetNode<V, T>, TranslationLink> links = new HashMap<>();
        Map<SynsetNode<V, T>, Integer> linkCounts = new HashMap<>();
        for (String word : node.getWords()) {
            Set<SynsetNode<V, T>> wTransSynsets = new HashSet<>();
            List<List<String>> translations = dict.translate(word);
            for (List<String> translation : translations) {
                Set<SynsetNode<V, T>> transSynsets = new HashSet<>();
                translation.stream().map(w -> repo.findNode(new Query(w, node.getPOS())))
                        .forEach(transSynsets::addAll);
                transSynsets.forEach(s -> {
                    wTransSynsets.add(s);
                    TranslationLink link = new TranslationLink(word, translation, jaccardIndex(s.getWords(), translation));
                    TranslationLink oldLink = links.get(s);
                    if (oldLink == null || oldLink.getWeight() < link.getWeight()) {
                        links.put(s, link);
                    }
                });
            }
            wTransSynsets.forEach(s -> linkCounts.put(s, 1 + linkCounts.getOrDefault(s, 0)));
        }
        linkCounts.entrySet().forEach(e -> {
            TranslationLink link = links.get(e.getKey());
            if (link != null) {
                double newWeight = link.getWeight() * p1Measure(e.getValue(), node.getWords().size());
                link.setWeight(newWeight);
            }
        });
        node.getEdges().putAll(links);
    }

    private double p1Measure(int wordsLinked, int wordsTotal) {
        double x = ((double) wordsLinked) / wordsTotal;
        if (x >= params.getP1Mean()) {
            return 1.0;
        } else {
            return p1ND.density(1 - x) * p1C;
        }
    }

    public double jaccardIndex(Set<String> words1, Collection<String> words2) {
        int commonCount = 0;
        for (String t : words2) {
            if (words1.contains(t)) {
                commonCount++;
            }
        }
        return 2.0d * commonCount / (words1.size() + words2.size());
    }
}
