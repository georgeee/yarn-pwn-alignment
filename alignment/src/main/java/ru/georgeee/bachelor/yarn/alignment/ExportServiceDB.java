package ru.georgeee.bachelor.yarn.alignment;

import edu.mit.jwi.item.ISynset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.georgeee.bachelor.yarn.clustering.Cluster;
import ru.georgeee.bachelor.yarn.core.GraphSettings;
import ru.georgeee.bachelor.yarn.core.SynsetNode;
import ru.georgeee.bachelor.yarn.core.TranslationLink;
import ru.georgeee.bachelor.yarn.db.entity.PwnSynset;
import ru.georgeee.bachelor.yarn.db.entity.Synset;
import ru.georgeee.bachelor.yarn.db.entity.TranslateEdge;
import ru.georgeee.bachelor.yarn.db.entity.YarnSynset;
import ru.georgeee.bachelor.yarn.db.repo.SynsetRepository;
import ru.georgeee.bachelor.yarn.db.repo.TranslateEdgeRepository;
import ru.georgeee.bachelor.yarn.xml.SynsetEntry;

import java.util.Map;
import java.util.function.Supplier;

@Component
class ExportServiceDB {
    private static final Logger log = LoggerFactory.getLogger(ExportServiceDB.class);
    @Autowired
    private GraphSettings grSettings;
    @Autowired
    private TranslateEdgeRepository translateEdgeRepository;
    @Autowired
    private SynsetRepository synsetRepository;

    private TranslateEdge getOrCreateEdge(PwnSynset pwnSynset, YarnSynset yarnSynset) {
        TranslateEdge edge = translateEdgeRepository.findByPwnSynsetAndYarnSynset(pwnSynset, yarnSynset);
        if (edge == null) {
            edge = new TranslateEdge();
            edge.setYarnSynset(yarnSynset);
            edge.setPwnSynset(pwnSynset);
        }
        return edge;
    }

    private <T extends Synset> T getOrCreateNode(String externalId, Supplier<T> factory) {
        T synset = (T) synsetRepository.findByExternalId(externalId);
        if (synset == null) {
            synset = factory.get();
            synset.setExternalId(externalId);
            synsetRepository.save(synset);
        }
        return synset;
    }

    private TranslateEdge addEdge(PwnSynset pwnSynset, SynsetNode<SynsetEntry, ISynset> yarnNode, double lWeight, double rWeight, TranslateEdge masterEdge) {
        if (lWeight < grSettings.getThreshold() || rWeight < grSettings.getThreshold()) {
            return null;
        }
        double weight = (lWeight + rWeight) / 2;
        log.info("Adding edge : {} {}, dir={} rev={}", pwnSynset.getExternalId(), yarnNode.getId(), lWeight, rWeight);
        YarnSynset yarnSynset = getOrCreateNode(yarnNode.getId(), YarnSynset::new);
        TranslateEdge edge = getOrCreateEdge(pwnSynset, yarnSynset);
        edge.setWeight(weight);
        edge.setMasterEdge(masterEdge);
        pwnSynset.getTranslateEdges().add(edge);
        return edge;
    }

    private double getRWeight(SynsetNode<?, ?> lNode, SynsetNode<?, ?> rNode) {
        TranslationLink rLink = rNode.getEdges().get(lNode);
        double rWeight = rLink == null ? 0 : rLink.getWeight();
        return rWeight;
    }

    @Transactional
    public void exportToDb(SynsetNode<ISynset, SynsetEntry> pwnNode) {
        PwnSynset pwnSynset = getOrCreateNode(pwnNode.getId(), PwnSynset::new);
        pwnSynset.getTranslateEdges().clear();
        for (Map.Entry<SynsetNode<SynsetEntry, ISynset>, TranslationLink> e : pwnNode.getEdges().entrySet()) {
            SynsetNode<SynsetEntry, ISynset> yarnNode = e.getKey();
            if (yarnNode instanceof Cluster) {
                TranslateEdge first = null;
                for (Cluster.Member<SynsetEntry, ISynset> member : (Cluster<SynsetEntry, ISynset>) yarnNode) {
                    TranslateEdge edge = addEdge(pwnSynset, member.getNode(), member.getWeight(), member.getRWeight(), first);
                    if (first == null) first = edge;
                }
            } else {
                TranslationLink link = e.getValue();
                addEdge(pwnSynset, yarnNode, link.getWeight(), getRWeight(pwnNode, yarnNode), null);
            }
        }
        synsetRepository.save(pwnSynset);
    }
}
