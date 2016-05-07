package ru.georgeee.bachelor.yarn.app;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.georgeee.bachelor.yarn.Yarn;
import ru.georgeee.bachelor.yarn.core.*;
import ru.georgeee.bachelor.yarn.xml.SynsetEntry;
import ru.georgeee.bachelor.yarn.xml.WordEntry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class YarnNodeRepository<V> extends NodeRepository<SynsetEntry, V> {
    private static final Logger log = LoggerFactory.getLogger(YarnNodeRepository.class);
    private final Yarn yarn;

    public YarnNodeRepository(Yarn yarn) {
        this.yarn = yarn;
    }

    public static POS getPOS(WordEntry wordEntry) {
        String grammar = wordEntry.getGrammar();
        if (grammar.indexOf('n') != -1) {
            return POS.NOUN;
        } else if (grammar.indexOf('v') != -1) {
            return POS.VERB;
        } else if (grammar.indexOf('a') != -1) {
            return POS.ADJECTIVE;
        }
        log.warn("Unknown grammar {} for {} (can't extract POS)", grammar, Yarn.toString(wordEntry));
        return null;
    }

    @Override
    public List<SynsetNode<SynsetEntry, V>> findNode(Query query) {
        return yarn.getWord(query.getWord()).stream()
                .filter(w -> getPOS(w.getWordEntry()) == query.getPos() || query.getPos() == null)
                .flatMap(w -> w.getSynsets().stream().map(Yarn.WordSynsetEntry::getSynset))
                .map(this::getNode)
                .collect(Collectors.toList());
    }

    private String determineGloss(SynsetEntry synset) {
        for (SynsetEntry.Word w : synset.getWord()) {
            if (w == null || w.getRef() == null) {
            } else {
                for (SynsetEntry.Word.Definition def : w.getDefinition()) {
                    if (StringUtils.isNotEmpty(def.getValue())) {
                        return def.getValue();
                    }
                }
            }
        }
        return null;
    }

    private POS determinePOS(SynsetEntry synset) {
        for (SynsetEntry.Word word : synset.getWord()) {
            if (word.getRef() != null) {
                return YarnNodeRepository.getPOS((WordEntry) word.getRef());
            }
        }
//                log.warn("Discrepancy in data: synset without a word: " + this);
        return POS.NOUN;
    }

    @Override
    protected SynsetNode<SynsetEntry, V> createNode(String id) {
        SynsetEntry synset = yarn.getSynset(id);
        if (synset == null) return null;
        String gloss = determineGloss(synset);
        Set<String> words = new HashSet<>();
        for (SynsetEntry.Word w : synset.getWord()) {
            if (w == null || w.getRef() == null) {
            } else {
                words.add(((WordEntry) w.getRef()).getWord());
            }
        }
        POS pos = determinePOS(synset);
        return new SynsetNode<SynsetEntry, V>() {
            @Override
            public String getGloss() {
                return gloss;
            }

            @Override
            public String getId() {
                return synset.getId();
            }

            @Override
            public Set<String> getWords() {
                return words;
            }

            @Override
            public List<WordData> getWordsWithData() {
                List<WordData> words = new ArrayList<>();
                for (SynsetEntry.Word w : synset.getWord()) {
                    if (w == null || w.getRef() == null) {
//                        log.warn("Discrepancy in data: {}", Yarn.toString(synset));
                    } else {
                        WordEntry we = (WordEntry) w.getRef();
                        WordData wordData = new WordData();
                        wordData.setLemma(we.getWord());
                        wordData.setGlosses(w.getDefinition()
                                .stream()
                                .map(SynsetEntry.Word.Definition::getValue)
                                .filter(StringUtils::isNotBlank)
                                .collect(Collectors.toList()));
                        wordData.setExamples(w.getExample().stream().map(SynsetEntry.Word.Example::getValue).collect(Collectors.toList()));
                        words.add(wordData);
                    }
                }
                return words;
            }

            @Override
            public POS getPOS() {
                return pos;
            }

            @Override
            public String toString() {
                return "Yarn " + super.toString();
            }
        };
    }

    @Override
    protected String getId(SynsetEntry data) {
        return data.getId();
    }
}
