package ru.georgeee.bachelor.yarn.app;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.georgeee.bachelor.yarn.core.*;
import ru.georgeee.bachelor.yarn.dict.Dict;
import ru.georgeee.bachelor.yarn.dict.StatTrackingDict;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class Metrics {
    private static final Logger log = LoggerFactory.getLogger(Metrics.class);
    @Autowired
    private MetricsParams params;
    private NormalDistribution p1ND;
    private double p1C;

    @PostConstruct
    private void init() {
        p1ND = new NormalDistribution(null, 1 - params.getP1Mean(), params.getP1Sd());
        p1C = 1 / p1ND.density(params.getP1Mean());
    }

    public <T, V> void processNode(GraphSettings grSettings, Dict dict, NodeRepository<V, T> repo, SynsetNode<T, V> node) {
        Map<SynsetNode<V, T>, TranslationLink> links = new HashMap<>();
        Map<SynsetNode<V, T>, Integer> linkCounts = new HashMap<>();
        for (WordData wordData : node.getWordsWithData()) {
            Set<SynsetNode<V, T>> wTransSynsets = new HashSet<>();
            String word = wordData.getLemma();
            List<Dict.Translation> translations = dict.translate(word);
            boolean matchAny = false;
            for (Dict.Translation translation : translations) {
                Set<SynsetNode<V, T>> transSynsets = new HashSet<>();
                translation
                        .getWords()
                        .stream().map(w -> repo.findNode(new Query(w, node.getPOS())))
                        .forEach(transSynsets::addAll);
                transSynsets.forEach(s -> {
                    wTransSynsets.add(s);
                    double weight = measureTrWeight(node, s, wordData, translation);
                    TranslationLink link = new TranslationLink(wordData, translation, weight);
                    TranslationLink oldLink = links.get(s);
                    if (oldLink == null || oldLink.getWeight() < link.getWeight()) {
                        links.put(s, link);
                    }
                });
                matchAny |= !transSynsets.isEmpty();
            }
            if (dict instanceof StatTrackingDict) {
                ((StatTrackingDict) dict).reportResult(word, matchAny);
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
        if (log.isDebugEnabled()) {
            log.debug("Node {}, all links: {}", node.getId(), links.keySet().stream().map(SynsetNode::getId).collect(Collectors.toList()));
        }
        Stream<Map.Entry<SynsetNode<V, T>, TranslationLink>> stream = links.entrySet().stream()
                .filter(e -> e.getValue().getWeight() >= grSettings.getThreshold());
        if (grSettings.getMaxEdges() > 0) {
            stream = stream
                    .sorted((a, b) -> -Double.compare(a.getValue().getWeight(), b.getValue().getWeight()))
                    .limit(grSettings.getMaxEdges());
        }
        stream.forEach(e -> {
            e.getKey().reportBackEdgeWeight(e.getValue().getWeight());
            node.getEdges().put(e.getKey(), e.getValue());
        });
        if (log.isDebugEnabled()) {
            log.debug("Node {}, resulting links: {}", node.getId(), node.getEdges().keySet().stream().map(SynsetNode::getId).collect(Collectors.toList()));
        }
    }

    /**
     * Measure initial weight of translation
     * I.e. weight of linkage between srcNode and destNode,
     * based only on single translation of single word from srcNode
     * (not concerning other translations/words/synsets)
     *
     * @param srcNode
     * @param destNode
     * @param wd
     * @param translation
     * @return weight of translation, in range [0..1]
     */
    private <T, V> double measureTrWeight(SynsetNode<T, V> srcNode, SynsetNode<V, T> destNode, WordData wd, Dict.Translation translation) {
        double weight = jaccardIndex(destNode.getWords(), translation.getWords());
        double tgW = 0;
        double tgBase = params.getTgBase();
        String tg = translation.getGloss();
        if (tg != null) {
            if (wd.getGlosses() != null) {
                for (String gloss : wd.getGlosses()) {
                    tgW = Math.max(tgW, tgMeasure(gloss, tg));
                }
            }
            if (srcNode.getGloss() != null) {
                tgW = Math.max(tgW, tgMeasure(srcNode.getGloss(), tg));
            }
        }
        weight = tgBase * weight + tgW * (1 - tgBase);
        return weight;
    }

    private double tgMeasure(String srcGloss, String trGloss) {
        //@TODO normalize words, retain only nouns, verbs, adjs (via steamer)
        Set<String> sgWords = extractWords(srcGloss);
        Set<String> tgWords = extractWords(trGloss);
        int total = tgWords.size();
        tgWords.retainAll(sgWords);
        int retained = tgWords.size();
        return ((double) retained) / total;
    }

    private static final Pattern WORD_PATTERN = Pattern.compile("\\w+", Pattern.UNICODE_CHARACTER_CLASS);

    private static Set<String> extractWords(String s) {
        Matcher m = WORD_PATTERN.matcher(s);
        Set<String> words = new HashSet<>();
        while (m.find()) {
            words.add(m.group().trim().toLowerCase());
        }
        return words;
    }

    /**
     * Penalty function.
     * If wordsLinked/wordsTotal is less than p1Mean, it decreases (otherwise it equals to 1.
     * Penalty purpose is to discard situation when a large synset A has a strong linkage to other (B),
     * but only by a few of it's words (which is suspicious)
     *
     * @param wordsLinked How many words from synset A have translations, that link it to some synset B
     * @param wordsTotal  Total amount of words in synset A
     * @return multiplier for weight, in range (0..1]
     */
    private double p1Measure(int wordsLinked, int wordsTotal) {
        double x = ((double) wordsLinked) / wordsTotal;
        double c = 1.0;
        if (x < params.getP1Mean()) {
            c = p1ND.density(1 - x) * p1C;
        }
        if (c > 1) {
            log.warn("p1Measure: linked={} total={} c={} (x={})", wordsLinked, wordsTotal, c, x);
        }
        return c;
    }

    public double jaccardIndex(Set<String> words1, Set<String> words2) {
        int commonCount = 0;
        for (String t : words2) {
            if (words1.contains(t)) {
                commonCount++;
            }
        }
        return 2.0d * commonCount / (words1.size() + words2.size());
    }
}
