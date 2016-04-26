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
import ru.georgeee.bachelor.yarn.app.Metrics;
import ru.georgeee.bachelor.yarn.app.PWNNodeRepository;
import ru.georgeee.bachelor.yarn.core.Query;
import ru.georgeee.bachelor.yarn.core.SynsetNode;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.bind.JAXBException;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootApplication(scanBasePackages = {"ru.georgeee.bachelor.yarn"})
public class Application implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(Application.class);
    private static final Gson gson = createGSON();
    @Value("${pwn.dest}")
    private String destPwnHomePath;
    @Value("${pwn.src}")
    private String srcPwnHomePath;
    @Value("${input}")
    private String inputFilePath;
    @Value("${result}")
    private String resultFile;
    @Value("${limit:5}")
    private int limit;
    @Value("${verbose:}")
    private String verboseDestFile;
    @Value("${jcThreshold:0.8}")
    private double jaccardThreshold;
    @Value("${lvThreshold:0.2}")
    private double lvDistThreshold;

    private final Map<String, String> result = new HashMap<>();

    @Autowired
    private Metrics metrics;
    private PWNNodeRepository<?> srcNodeRepository;
    private PWNNodeRepository<?> destNodeRepository;

    private BufferedWriter resultWriter;

    private Scanner scanner;

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

    private double lvDistNormalized(String a, String b) {
        return ((double) StringUtils.getLevenshteinDistance(a, b)) / (a.length() + b.length());
    }

    @PostConstruct
    private void init() throws IOException, JAXBException {
        IDictionary destPwnDict = new Dictionary(new URL("file", null, destPwnHomePath));
        destPwnDict.open();
        IDictionary srcPwnDict = new Dictionary(new URL("file", null, srcPwnHomePath));
        srcPwnDict.open();
        srcNodeRepository = new PWNNodeRepository<>(srcPwnDict);
        destNodeRepository = new PWNNodeRepository<>(destPwnDict);
        try {
            Files.lines(Paths.get(resultFile)).forEach(s -> {
                String[] parts = s.split(":", 2);
                String left = parts[0].trim();
                String right = parts[1].trim();
                if (right.equals("null")) {
                    right = null;
                }
                result.put(left, right);
            });
        } catch (NoSuchFileException e) {
            log.info("No file {} exist", resultFile);
        }
        resultWriter = Files.newBufferedWriter(Paths.get(resultFile), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        scanner = new Scanner(System.in);
    }

    @PreDestroy
    private void close() throws IOException {
        resultWriter.close();
    }

    private void addResult(String origId, String id) throws IOException {
        String oldId = result.put(origId, id);
        if (oldId != null) {
            log.warn("Duplicate for origId {} ({}, {})", oldId, id);
        }
        resultWriter.append(origId).append(": ").append(id);
        resultWriter.newLine();
        resultWriter.flush();
        log.info("Added result origId={} id={}", origId, id);
    }

    @Override
    public void run(String... args) throws IOException {
        Map<String, SrcEntry> verboseResult = new HashMap<>();
        try (BufferedReader br = Files.newBufferedReader(Paths.get(inputFilePath))) {
            String origId;
            while ((origId = br.readLine()) != null) {
                origId = origId.trim();
                if (origId.isEmpty()) continue;
                if (result.containsKey(origId)) {
                    continue;
                }
                Pair<String, Collection<Pair<Double, String>>> ids = processId(origId);
                String matchedByGlossId = ids.getLeft();
                if (matchedByGlossId != null) {
                    addResult(origId, matchedByGlossId);
                } else {
                    SrcEntry entry = prepareVerboseIds(origId, ids.getRight());
                    if (entry != null) {
                        verboseResult.put(origId, entry);
                    }
                }
            }
        }
        if (StringUtils.isNotEmpty(verboseDestFile)) {
            Files.write(Paths.get(verboseDestFile), Collections.singletonList(gson.toJson(verboseResult)));
        }
        int counter = 0;
        int count = verboseResult.size();
        for (Map.Entry<String, SrcEntry> e : verboseResult.entrySet()) {
            System.out.printf("[%d / %d] %s: gloss=%s%n\t\twords=%s%n", ++counter, count, e.getKey(), e.getValue().getGloss(), e.getValue().getWords());
            List<DestEntry> options = e.getValue().getEntries();
            System.out.printf("\t% 2d: <omit entry>%n", 0);
            for (int i = 0; i < options.size(); ++i) {
                DestEntry option = options.get(i);
                System.out.printf("\t% 2d: %s jc=%.3f lv=%.3f gloss=%s%n\t\twords=%s%n", i + 1, option.getId(), option.getJaccardIndex(), option.getLvDistNormalized(), option.getGloss(), option.getWords());
            }
            int optI = Integer.MIN_VALUE;
            while (optI < 0 || optI > options.size()) {
                if (optI != Integer.MIN_VALUE) {
                    System.out.println("Wrong index " + optI);
                }
                System.out.print("Answer > ");
                optI = scanner.nextInt();
            }
            addResult(e.getKey(), optI == 0 ? null : options.get(optI - 1).getId());
        }
    }

    private SrcEntry prepareVerboseIds(String id, Collection<Pair<Double, String>> ids) {
        SynsetNode<ISynset, ?> node = srcNodeRepository.getNodeById(id);
        if (node == null) {
            log.warn("Synset {} doesn't exist", id);
            return null;
        }
        List<DestEntry> entries = ids.stream().map(e -> prepareVerbose(node, e)).collect(Collectors.toList());
        Collections.sort(entries, (a, b) -> -Double.compare(a.getJaccardIndex(), b.getJaccardIndex()));
        return new SrcEntry(node.getGloss(), node.getWords(), entries);
    }

    private DestEntry prepareVerbose(SynsetNode<ISynset, ?> origNode, Pair<Double, String> e) {
        SynsetNode<ISynset, ?> node = destNodeRepository.getNodeById(e.getRight());
        return new DestEntry(e.getLeft(), e.getRight(), node.getWords(), node.getGloss(), lvDistNormalized(origNode.getGloss(), node.getGloss()));
    }

    private Pair<String, Collection<Pair<Double, String>>> processId(String id) {
        SynsetNode<ISynset, ?> srcNode = srcNodeRepository.getNodeById(id);

        if (srcNode == null) {
            log.warn("No node found in src dict: {}", id);
            return new ImmutablePair<>(null, Collections.emptyList());
        } else {
            Set<String> used = new HashSet<>();
            PriorityQueue<Pair<Double, String>> heap = new PriorityQueue<>();
            boolean matchedByGlossInited = false;
            boolean matchedByGlossZeroLv = false;
            String matchedByGlossId = null;
            for (String word : srcNode.getWords()) {
                for (SynsetNode<ISynset, ?> destNode : destNodeRepository.findNode(new Query(word, srcNode.getPOS()))) {
                    if (!used.add(destNode.getId())) continue;
                    double lvDistNormalized = lvDistNormalized(srcNode.getGloss(), destNode.getGloss());
                    double jaccard = metrics.jaccardIndex(srcNode.getWords(), destNode.getWords());
                    if (lvDistNormalized <= 1e-5) {
                        matchedByGlossZeroLv = true;
                        matchedByGlossId = destNode.getId();
                    } else if (!matchedByGlossZeroLv && (lvDistNormalized <= lvDistThreshold && jaccard >= jaccardThreshold)) {
                        if (!matchedByGlossInited) {
                            matchedByGlossId = destNode.getId();
                            matchedByGlossInited = true;
                        } else {
                            log.info("Two with equal glosses: {} {} (original {})", matchedByGlossId, destNode.getId(), srcNode.getId());
                            matchedByGlossId = null;
                        }
                    }
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
            return new ImmutablePair<>(matchedByGlossId, heap);
        }
    }

    @Getter
    private static class SrcEntry {
        private final String gloss;
        private final Collection<String> words;
        private final List<DestEntry> entries;

        private SrcEntry(String gloss, Collection<String> words, List<DestEntry> entries) {
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
        private final double lvDistNormalized;

        private DestEntry(double jaccardIndex, String id, Collection<String> words, String gloss, double lvDistNormalized) {
            this.jaccardIndex = jaccardIndex;
            this.id = id;
            this.words = words;
            this.gloss = gloss;
            this.lvDistNormalized = lvDistNormalized;
        }
    }
}