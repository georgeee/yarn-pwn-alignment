package ru.georgeee.bachelor.yarn.alignment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.georgeee.bachelor.yarn.Yarn;
import ru.georgeee.bachelor.yarn.graph.NodeRepository;
import ru.georgeee.bachelor.yarn.graph.Query;
import ru.georgeee.bachelor.yarn.graph.SynsetNode;
import ru.georgeee.bachelor.yarn.xml.SynsetEntry;
import ru.georgeee.bachelor.yarn.xml.WordEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class YarnNodeRepository<V> extends NodeRepository<SynsetEntry, V> {
    private static final Logger log = LoggerFactory.getLogger(YarnNodeRepository.class);
    private final Yarn yarn;

    public YarnNodeRepository(Yarn yarn) {
        this.yarn = yarn;
    }

    @Override
    public List<SynsetNode<SynsetEntry, V>> findNode(Query query) {
        return yarn.getWord(query.getWord()).stream()
                .filter(w -> w.getPOS() == query.getPos() || query.getPos() == null)
                .flatMap(w -> w.getSynsets().stream().map(Yarn.WordSynsetEntry::getSynset))
                .map(this::getNode)
                .collect(Collectors.toList());
    }

    @Override
    protected SynsetNode<SynsetEntry, V> createNode(SynsetEntry data) {
        return new SynsetNode<SynsetEntry, V>(data) {
            @Override
            public String getId() {
                return data.getId();
            }

            @Override
            public List<String> getWords() {
                List<String> words = new ArrayList<>();
                for (SynsetEntry.Word w : data.getWord()) {
                    if (w == null || w.getRef() == null) {
                        log.warn("Discrepancy in data: {}", Yarn.toString(data));
                    } else {
                        words.add(((WordEntry) w.getRef()).getWord());
                    }
                }
                return words;
            }

            @Override
            public POS getPOS() {
                return ru.georgeee.bachelor.yarn.Yarn.getPOS((WordEntry) data.getWord().get(0).getRef());
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
