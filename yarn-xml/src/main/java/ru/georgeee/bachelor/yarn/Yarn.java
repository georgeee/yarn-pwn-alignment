package ru.georgeee.bachelor.yarn;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.georgeee.bachelor.yarn.xml.SynsetEntry;
import ru.georgeee.bachelor.yarn.xml.WordEntry;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    public SynsetEntry getSynset(String id) {
        return synsetByIdMap.get(id);
    }

    public List<Word> getWord(String value) {
        return wordByValueMap.getOrDefault(value, Collections.emptyList());
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

    public static String toString(SynsetEntry.Word word) {
        if (word == null) return "null";
        WordEntry ref = (WordEntry) word.getRef();
//        return "{" + toString(ref) + " def=" + word.getDefinition().stream().map(SynsetEntry.Word.Definition::getValue).collect(Collectors.toList()) + "}";
        return toString(ref);
    }

    public static String toString(WordEntry ref) {
        if (ref == null) return "null";
        return "{ " + ref.getId() + " : " + ref.getWord() + " (" + ref.getGrammar() + ") }";
    }

    @SuppressWarnings("unchecked")
    private static <T> T unmarshal(Class<T> docClass, InputStream inputStream)
            throws JAXBException {
        String packageName = docClass.getPackage().getName();
        JAXBContext jc = JAXBContext.newInstance(packageName);
        Unmarshaller u = jc.createUnmarshaller();
        return (T) u.unmarshal(inputStream);
    }

    public static Yarn create(Path yarnXmlPath) throws IOException, JAXBException {
        return new Yarn(unmarshal(ru.georgeee.bachelor.yarn.xml.Yarn.class, Files.newInputStream(yarnXmlPath)));
    }

}
