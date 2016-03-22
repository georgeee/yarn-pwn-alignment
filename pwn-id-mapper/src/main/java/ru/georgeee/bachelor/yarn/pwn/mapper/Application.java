package ru.georgeee.bachelor.yarn.pwn.mapper;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.*;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.georgeee.bachelor.yarn.alignment.Metrics;
import ru.georgeee.bachelor.yarn.alignment.PWNNodeRepository;
import ru.georgeee.bachelor.yarn.graph.Query;
import ru.georgeee.bachelor.yarn.graph.SynsetNode;


import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBException;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

@SpringBootApplication(scanBasePackages = {"ru.georgeee.bachelor.yarn"})
public class Application implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(Application.class);
    private static final Gson gson = createGSON();
    @Value("${pwn.dest}")
    private String destPwnHomePath;
    @Value("${pwn.src}")
    private String srcPwnHomePath;
    @Value("${input:}")
    private String inputFilePath;
    @Value("${limit:5}")
    private int limit;
    @Value("${verbose:}")
    private String verboseDestFile;

    @Autowired
    private Metrics metrics;
    private PWNNodeRepository<?> srcNodeRepository;
    private PWNNodeRepository<?> destNodeRepository;

    private static Gson createGSON() {
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        return builder.create();
    }

    public static void main(String[] args) throws Exception {
        SpringApplication application = new SpringApplication(Application.class);
        application.setApplicationContextClass(AnnotationConfigApplicationContext.class);
        SpringApplication.run(Application.class, args);
    }

    @PostConstruct
    private void init() throws IOException, JAXBException {
        IDictionary destPwnDict = new Dictionary(new URL("file", null, destPwnHomePath));
        destPwnDict.open();
        IDictionary srcPwnDict = new Dictionary(new URL("file", null, srcPwnHomePath));
        srcPwnDict.open();
        srcNodeRepository = new PWNNodeRepository<>(srcPwnDict);
        destNodeRepository = new PWNNodeRepository<>(destPwnDict);
    }

    @Override
    public void run(String... args) throws IOException {
        Map<String, Pair<Double, String>> result = new HashMap<>();
        Map<String, SrcEntry> verboseResult = new HashMap<>();
        try (BufferedReader br = openInput(inputFilePath)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (result.containsKey(line)) {
                    continue;
                }
                Collection<Pair<Double, String>> ids = processId(line);
                Pair<Double, String> winner = ids.stream().max(Pair::compareTo).orElse(null);
                result.put(line, winner);
                if (StringUtils.isNotEmpty(verboseDestFile)) {
                    verboseResult.put(line, prepareVerboseIds(line, ids));
                }
            }
        }
        if (StringUtils.isNotEmpty(verboseDestFile)) {
            Files.write(Paths.get(verboseDestFile), Collections.singletonList(gson.toJson(verboseResult)));
        }
        for (Map.Entry<String, Pair<Double, String>> e : result.entrySet()) {
            System.out.println(e.getKey() + ": " + (e.getValue() == null ? null : e.getValue().getLeft() + " " + e.getValue().getRight()));
        }
    }

    private SrcEntry prepareVerboseIds(String id, Collection<Pair<Double, String>> ids) {
        SynsetNode<ISynset, ?> node = srcNodeRepository.getNodeById(id);
        List<DestEntry> entries = ids.stream().map(this::prepareVerbose).collect(Collectors.toList());
        Collections.sort(entries, (a, b) -> -Double.compare(a.getJaccardIndex(), b.getJaccardIndex()));
        return new SrcEntry(node.getGloss(), node.getWords(), entries);
    }

    private DestEntry prepareVerbose(Pair<Double, String> e) {
        SynsetNode<ISynset, ?> node = destNodeRepository.getNodeById(e.getRight());
        return new DestEntry(e.getLeft(), e.getRight(), node.getWords(), node.getGloss());
    }

    private Collection<Pair<Double, String>> processId(String id) {
        SynsetNode<ISynset, ?> srcNode = srcNodeRepository.getNodeById(id);
        if (srcNode == null) {
            log.warn("No node found in src dict: {}", id);
            return Collections.emptyList();
        } else {
            Set<String> used = new HashSet<>();
            PriorityQueue<Pair<Double, String>> heap = new PriorityQueue<>();
            for (String word : srcNode.getWords()) {
                for (SynsetNode<ISynset, ?> destNode : destNodeRepository.findNode(new Query(word, srcNode.getPOS()))) {
                    if (!used.add(destNode.getId())) continue;
                    double jaccard = metrics.jaccardIndex(srcNode.getWords(), destNode.getWords());
                    if (heap.size() < limit) {
                        heap.add(new ImmutablePair<>(jaccard, destNode.getId()));
                    } else {
                        Pair<Double, String> min = heap.peek();
                        if (min.getLeft() < jaccard) {
                            heap.remove();
                            heap.add(new ImmutablePair<>(jaccard, destNode.getId()));
                        }
                    }
                }
            }
            return heap;
        }
    }

    private BufferedReader openInput(String inputFilePath) throws IOException {
        if (StringUtils.isEmpty(inputFilePath) || inputFilePath.equals("-")) {
            return new BufferedReader(new InputStreamReader(System.in));
        }
        return Files.newBufferedReader(Paths.get(inputFilePath));
    }

    @Getter
    private static class SrcEntry {
        private final String gloss;
        private final Collection<String> words;
        private final Collection<DestEntry> entries;

        private SrcEntry(String gloss, Collection<String> words, Collection<DestEntry> entries) {
            this.gloss = gloss;
            this.words = words;
            this.entries = entries;
        }
    }

    @Getter
    private static class DestEntry {
        private final double jaccardIndex;
        private final String id;
        private final Collection<String> words;
        private final String gloss;

        private DestEntry(double jaccardIndex, String id, Collection<String> words, String gloss) {
            this.jaccardIndex = jaccardIndex;
            this.id = id;
            this.words = words;
            this.gloss = gloss;
        }
    }
}