package ru.georgeee.bachelor.yarn.app;

import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.*;
import edu.mit.jwi.item.POS;
import org.apache.commons.lang3.StringUtils;
import ru.georgeee.bachelor.yarn.core.*;

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
            for (ru.georgeee.bachelor.yarn.core.POS pos : ru.georgeee.bachelor.yarn.core.POS.values()) {
                result.addAll(findNodeImpl(query.getWord(), pos));
            }
            return result;
        } else {
            return findNodeImpl(query.getWord(), query.getPos());
        }
    }

    private List<SynsetNode<ISynset, V>> findNodeImpl(String word, ru.georgeee.bachelor.yarn.core.POS pos) {
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

    private POS convertPOS(ru.georgeee.bachelor.yarn.core.POS pos) {
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

    private ISynset getSynset(String id) {
        try {
            return pwnDict.getSynset(SynsetID.parseSynsetID(id));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String getPWNLemma(IWord w) {
        return w.getLemma().replace('_', ' ');
    }

    private ru.georgeee.bachelor.yarn.core.POS determinePos(POS pos) {
        if (pos != null) {
            switch (pos) {
                case NOUN:
                    return ru.georgeee.bachelor.yarn.core.POS.NOUN;
                case VERB:
                    return ru.georgeee.bachelor.yarn.core.POS.VERB;
                case ADJECTIVE:
                    return ru.georgeee.bachelor.yarn.core.POS.ADJECTIVE;
                case ADVERB:
                    return ru.georgeee.bachelor.yarn.core.POS.ADVERB;
            }
        }
        return null;
    }

    @Override
    protected SynsetNode<ISynset, V> createNode(String id) {
        ISynset synset = getSynset(id);
        if (synset == null) return null;
        Set<String> words = synset.getWords().stream()
                .map(PWNNodeRepository::getPWNLemma).collect(Collectors.toSet());
        ru.georgeee.bachelor.yarn.core.POS pos = determinePos(synset.getPOS());
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
                return words;
            }

            @Override
            public List<WordData> getWordsWithData() {
                return synset.getWords().stream().map(w -> {
                    String lemma = getPWNLemma(w);
                    WordData wordData = new WordData();
                    wordData.setLemma(lemma);
                    return wordData;
                }).collect(Collectors.toList());
            }

            @Override
            public ru.georgeee.bachelor.yarn.core.POS getPOS() {
                return pos;
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
