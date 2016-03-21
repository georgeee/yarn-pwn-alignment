package ru.georgeee.bachelor.yarn;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.georgeee.bachelor.yarn.graph.SynsetNode;
import ru.georgeee.bachelor.yarn.xml.SynsetEntry;
import ru.georgeee.bachelor.yarn.xml.WordEntry;

import java.util.*;
import java.util.stream.Collectors;

public class Yarn {
    private static final Logger log = LoggerFactory.getLogger(Yarn.class);
    private final ru.georgeee.bachelor.yarn.xml.Yarn data;
    private final Map<String, List<Word>> wordByValueMap = new HashMap<>();
    private final Map<String, List<WordSynsetEntry>> synsetsByWordIdMap = new HashMap<>();
    private final Map<String, SynsetEntry> synsetByIdMap = new HashMap<>();

    public Yarn(ru.georgeee.bachelor.yarn.xml.Yarn data) {
        this.data = data;
        init();
    }

    private void init() {
        for (WordEntry word : data.getWords().getWordEntry()) {
            wordByValueMap.computeIfAbsent(word.getWord(), k -> new ArrayList<>());
            wordByValueMap.get(word.getWord()).add(new Word(word));
        }
        for (SynsetEntry synset : data.getSynsets().getSynsetEntry()) {
            synsetByIdMap.put(synset.getId(), synset);
            for (int i = 0; i < synset.getWord().size(); ++i) {
                SynsetEntry.Word word = synset.getWord().get(i);
                WordEntry wordEntry = (WordEntry) word.getRef();
                if (wordEntry == null) continue;
                synsetsByWordIdMap.computeIfAbsent(wordEntry.getId(), k -> new ArrayList<>());
                synsetsByWordIdMap.get(wordEntry.getId()).add(new WordSynsetEntry(i, synset));
            }
        }
    }

    public SynsetEntry getSynset(String id){
        return synsetByIdMap.get(id);
    }

    public List<Word> getWord(String value) {
        return wordByValueMap.getOrDefault(value, Collections.emptyList());
    }

    public static SynsetNode.POS getPOS(WordEntry wordEntry) {
        String grammar = wordEntry.getGrammar();
        if (grammar.indexOf('n') != -1) {
            return SynsetNode.POS.NOUN;
        } else if (grammar.indexOf('v') != -1) {
            return SynsetNode.POS.VERB;
        } else if (grammar.indexOf('a') != -1) {
            return SynsetNode.POS.ADJECTIVE;
        }
        log.warn("Unknown grammar {} for {} (can't extract POS)", grammar, toString(wordEntry));
        return null;
    }

    @Getter
    public class Word {
        private final WordEntry wordEntry;

        public Word(WordEntry wordEntry) {
            this.wordEntry = wordEntry;
        }

        public String getWord() {
            return wordEntry.getWord();
        }

        public String getId() {
            return wordEntry.getId();
        }

        public List<WordSynsetEntry> getSynsets() {
            return synsetsByWordIdMap.getOrDefault(getId(), Collections.emptyList());
        }

        public SynsetNode.POS getPOS() {
            return Yarn.getPOS(wordEntry);
        }

        public String toString() {
            return Yarn.toString(wordEntry);
        }
    }

    public static class WordSynsetEntry {
        @Getter
        private final int pos;
        @Getter
        private final SynsetEntry synset;

        @Override
        public String toString() {
            return "{synsetEntry " +
                    "pos=" + pos +
                    " synset=" + Yarn.toString(synset) +
                    '}';
        }

        public WordSynsetEntry(int pos, SynsetEntry synset) {
            this.pos = pos;
            this.synset = synset;
        }

        public SynsetEntry.Word getDetails() {
            return synset.getWord().get(pos);
        }
    }

    public static String toString(SynsetEntry synsetEntry) {
        return "{synset id=" + synsetEntry.getId() + ": "
                + synsetEntry.getWord().stream()
                .map(Yarn::toString)
                .collect(Collectors.toList()) + "}";
    }

    private static String toString(SynsetEntry.Word word) {
        if (word == null) return "null";
        WordEntry ref = (WordEntry) word.getRef();
//        return "{" + toString(ref) + " def=" + word.getDefinition().stream().map(SynsetEntry.Word.Definition::getValue).collect(Collectors.toList()) + "}";
        return toString(ref);
    }

    private static String toString(WordEntry ref) {
        if (ref == null) return "null";
        return "{ " + ref.getId() + " : " + ref.getWord() + " (" + ref.getGrammar() + ") }";
    }
}
