package ru.georgeee.bachelor.yarn;

import edu.mit.jwi.item.POS;
import lombok.Getter;
import ru.georgeee.bachelor.yarn.xml.SynsetEntry;
import ru.georgeee.bachelor.yarn.xml.WordEntry;

import java.util.*;
import java.util.stream.Collectors;

public class Yarn {
    private final ru.georgeee.bachelor.yarn.xml.Yarn data;
    private final Map<String, List<Word>> wordByValueMap;
    private final Map<String, List<WordSynsetEntry>> synsetByWordIdMap;

    public Yarn(ru.georgeee.bachelor.yarn.xml.Yarn data) {
        this.data = data;
        wordByValueMap = new HashMap<>();
        synsetByWordIdMap = new HashMap<>();
        init();
    }

    private void init() {
        for (WordEntry word : data.getWords().getWordEntry()) {
            wordByValueMap.computeIfAbsent(word.getWord(), k -> new ArrayList<>());
            wordByValueMap.get(word.getWord()).add(new Word(word));
        }
        for (SynsetEntry synset : data.getSynsets().getSynsetEntry()) {
            for (int i = 0; i < synset.getWord().size(); ++i) {
                SynsetEntry.Word word = synset.getWord().get(i);
                WordEntry wordEntry = (WordEntry) word.getRef();
                if (wordEntry == null) continue;
                synsetByWordIdMap.computeIfAbsent(wordEntry.getId(), k -> new ArrayList<>());
                synsetByWordIdMap.get(wordEntry.getId()).add(new WordSynsetEntry(i, synset));
            }
        }
    }

    public List<Word> getWord(String value) {
        return wordByValueMap.getOrDefault(value, Collections.emptyList());
    }

    public static POS getPOS(WordEntry wordEntry) {
        switch (wordEntry.getId().charAt(0)){
            case 'n' : return POS.NOUN;
            case 'v' : return POS.VERB;
            case 'a' : return POS.ADJECTIVE;
        }
        throw new IllegalArgumentException("Unknown POS for " + toString(wordEntry));
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
            return synsetByWordIdMap.getOrDefault(getId(), Collections.emptyList());
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
        WordEntry ref = (WordEntry) word.getRef();
//        return "{" + toString(ref) + " def=" + word.getDefinition().stream().map(SynsetEntry.Word.Definition::getValue).collect(Collectors.toList()) + "}";
        return toString(ref);
    }

    private static String toString(WordEntry ref) {
        return "{ " + ref.getId() + " : " + ref.getWord() + " (" + ref.getGrammar() + ") }";
    }
}
