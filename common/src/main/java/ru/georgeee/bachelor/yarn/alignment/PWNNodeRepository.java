package ru.georgeee.bachelor.yarn.alignment;

import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.*;
import org.apache.commons.lang3.StringUtils;
import ru.georgeee.bachelor.yarn.graph.Query;
import ru.georgeee.bachelor.yarn.graph.SynsetNode;
import ru.georgeee.bachelor.yarn.graph.NodeRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PWNNodeRepository<V> extends NodeRepository<ISynset, V> {
    private final IDictionary pwnDict;

    public PWNNodeRepository(IDictionary pwnDict) {
        this.pwnDict = pwnDict;
    }

    @Override
    public List<SynsetNode<ISynset, V>> findNode(Query query) {
        if (StringUtils.isEmpty(query.getWord())) {
            return Collections.emptyList();
        }
        if (query.getPos() == null) {
            List<SynsetNode<ISynset, V>> result = new ArrayList<>();
            for (SynsetNode.POS pos : SynsetNode.POS.values()) {
                result.addAll(findNodeImpl(query.getWord(), pos));
            }
            return result;
        } else {
            return findNodeImpl(query.getWord(), query.getPos());
        }
    }

    private List<SynsetNode<ISynset, V>> findNodeImpl(String word, SynsetNode.POS pos) {
        IIndexWord idxWord = pwnDict.getIndexWord(word, convertPOS(pos));
        if (idxWord == null) {
            return Collections.emptyList();
        } else {
            for (IWordID pwnWordID : idxWord.getWordIDs()) {
                IWord pwnWord = pwnDict.getWord(pwnWordID);
                pwnWord.getSynset();
            }
            return idxWord.getWordIDs().stream()
                    .map(pwnDict::getWord)
                    .map(IWord::getSynset)
                    .map(this::getNode)
                    .collect(Collectors.toList());
        }
    }

    private POS convertPOS(SynsetNode.POS pos) {
        if (pos != null) {
            switch (pos) {
                case NOUN:
                    return POS.NOUN;
                case VERB:
                    return POS.VERB;
                case ADJECTIVE:
                    return POS.ADJECTIVE;
                case ADVERB:
                    return POS.ADVERB;
            }
        }
        return null;
    }

    private ISynset getSynset(String id){
        try {
            return pwnDict.getSynset(SynsetID.parseSynsetID(id));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    protected SynsetNode<ISynset, V> createNode(String id) {
        ISynset synset = getSynset(id);
        if (synset == null) return null;
        return new SynsetNode<ISynset, V>() {
            @Override
            public String getGloss() {
                return synset.getGloss();
            }

            @Override
            public String getId() {
                return synset.getID().toString();
            }

            @Override
            public Set<String> getWords() {
                return synset.getWords().stream()
                        .map(w -> w.getLemma().replace('_', ' ')).collect(Collectors.toSet());
            }

            @Override
            public POS getPOS() {
                edu.mit.jwi.item.POS pos = synset.getPOS();
                if (pos != null) {
                    switch (pos) {
                        case NOUN:
                            return POS.NOUN;
                        case VERB:
                            return POS.VERB;
                        case ADJECTIVE:
                            return POS.ADJECTIVE;
                        case ADVERB:
                            return POS.ADVERB;
                    }
                }
                return null;
            }

            @Override
            public String toString() {
                return "PWN " + super.toString();
            }
        };
    }

    @Override
    protected String getId(ISynset data) {
        return data.getID().toString();
    }
}
