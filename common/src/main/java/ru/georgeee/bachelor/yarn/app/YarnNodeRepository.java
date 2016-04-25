package ru.georgeee.bachelor.yarn.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.georgeee.bachelor.yarn.Yarn;
import ru.georgeee.bachelor.yarn.core.POS;
import ru.georgeee.bachelor.yarn.core.Query;
import ru.georgeee.bachelor.yarn.core.SynsetNode;
import ru.georgeee.bachelor.yarn.core.NodeRepository;
import ru.georgeee.bachelor.yarn.xml.SynsetEntry;
import ru.georgeee.bachelor.yarn.xml.WordEntry;

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

    @Override
    protected SynsetNode<SynsetEntry, V> createNode(String id) {
        SynsetEntry synset = yarn.getSynset(id);
        if (synset == null) return null;
        return new SynsetNode<SynsetEntry, V>() {
            @Override
            public String getGloss() {
                return null;
            }

            @Override
            public String getId() {
                return synset.getId();
            }

            @Override
            public Set<String> getWords() {
                Set<String> words = new HashSet<>();
                for (SynsetEntry.Word w : synset.getWord()) {
                    if (w == null || w.getRef() == null) {
//                        log.warn("Discrepancy in data: {}", Yarn.toString(synset));
                    } else {
                        words.add(((WordEntry) w.getRef()).getWord());
                    }
                }
                return words;
            }

            @Override
            public POS getPOS() {
                for (SynsetEntry.Word word : synset.getWord()) {
                    if (word.getRef() != null) {
                        return YarnNodeRepository.getPOS((WordEntry) word.getRef());
                    }
                }
//                log.warn("Discrepancy in data: synset without a word: " + this);
                return POS.NOUN;
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
