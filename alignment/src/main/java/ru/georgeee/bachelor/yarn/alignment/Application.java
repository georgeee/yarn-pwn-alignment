package ru.georgeee.bachelor.yarn.alignment;


import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.*;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.georgeee.bachelor.yarn.Yarn;
import ru.georgeee.bachelor.yarn.graph.*;
import ru.georgeee.bachelor.yarn.xml.SynsetEntry;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@SpringBootApplication(scanBasePackages = {"ru.georgeee.bachelor.yarn"})
public class Application implements CommandLineRunner {

    @Value("${pwn.home}")
    private String pwnHomePath;

    @Value("${yarn.xml}")
    private String yarnXmlPath;

    @Value("${dict.en_ru}")
    private String enRuDictPath;

    @Value("${dict.ru_en}")
    private String ruEnDictPath;

    @Value("${out.dot:out.dot}")
    private String graphvizOutFile;

    @Autowired
    private DictFactory dictFactory;

    @Autowired
    private MetricsParams params;

    private SimpleDict enRuDict;
    private SimpleDict ruEnDict;

    private Yarn yarn;
    private IDictionary pwnDict;

    @Autowired
    private GraphVizSettings gvSettings;

    public static void main(String[] args) throws Exception {
        SpringApplication application = new SpringApplication(Application.class);
        application.setApplicationContextClass(AnnotationConfigApplicationContext.class);
        SpringApplication.run(Application.class, args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T unmarshal(Class<T> docClass, InputStream inputStream)
            throws JAXBException {
        String packageName = docClass.getPackage().getName();
        JAXBContext jc = JAXBContext.newInstance(packageName);
        Unmarshaller u = jc.createUnmarshaller();
        return (T) u.unmarshal(inputStream);
    }

    @PostConstruct
    private void init() throws IOException, JAXBException {
        enRuDict = dictFactory.getDict(enRuDictPath);
        ruEnDict = dictFactory.getDict(ruEnDictPath);
        yarn = new Yarn(unmarshal(ru.georgeee.bachelor.yarn.xml.Yarn.class, Files.newInputStream(Paths.get(yarnXmlPath))));
        pwnDict = new Dictionary(new URL("file", null, pwnHomePath));
        pwnDict.open();
    }

    @Override
    public void run(String... args) throws IOException, JAXBException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            String query;
            while ((query = br.readLine()) != null) {
                query = query.trim();
                if (query.isEmpty()) continue;
                if (query.charAt(0) == ':') {
                    int index = query.indexOf('=');
                    if (index == -1) {
                        System.err.println("Wrong format");
                    } else {
                        String key = query.substring(1, index).trim();
                        String value = query.substring(index + 1).trim();
                        processParameter(key, value);
                    }
                } else {
                    PWNNodeRepository<SynsetEntry> pwnRepo = new PWNNodeRepository<>(pwnDict);
                    YarnNodeRepository<ISynset> yarnRepo = new YarnNodeRepository<>(yarn);
                    List<SynsetNode<?, ?>> origSynsets = new ArrayList<>();
                    for (String s : query.split("\\s*,\\s*")) {
                        System.out.println("EnRu: " + s + ":" + enRuDict.translate(s));
                        System.out.println("RuEn: " + s + ":" + ruEnDict.translate(s));
                        List<SynsetNode<SynsetEntry, ISynset>> yarnSynsets = findNode(yarnRepo, s);
                        List<SynsetNode<ISynset, SynsetEntry>> pwnSynsets = findNode(pwnRepo, s);
                        origSynsets.addAll(yarnSynsets);
                        origSynsets.addAll(pwnSynsets);
                        yarnSynsets.stream().forEach(node -> processNode(ruEnDict, pwnRepo, node));
                        pwnSynsets.stream().forEach(node -> processNode(enRuDict, yarnRepo, node));
                    }
                    pwnRepo.getNodes().stream().forEach(node -> processNode(enRuDict, yarnRepo, node));
                    yarnRepo.getNodes().stream().forEach(node -> processNode(ruEnDict, pwnRepo, node));
                    printSynsets(origSynsets);
                }
            }
        }
    }

    private <T, V> void processNode(SimpleDict dict, NodeRepository<V, T> repo, SynsetNode<T, V> node) {
        Map<SynsetNode<V, T>, TranslationLink> links = new HashMap<>();
        for (String word : node.getWords()) {
            List<List<String>> translations = dict.translate(word);
            for (List<String> translation : translations) {
                Set<SynsetNode<V, T>> transSynsets = new HashSet<>();
                translation.stream().map(w -> repo.findNode(new Query(w, node.getPOS())))
                        .forEach(transSynsets::addAll);
                transSynsets.forEach(s -> {
                    TranslationLink link = composeLink(word, s, translation);
                    if (link.getWeight() >= params.getCutTh()) {
                        TranslationLink oldLink = links.get(s);
                        if (oldLink == null || oldLink.getWeight() < link.getWeight()) {
                            links.put(s, link);
                        }
                    }
                });
            }
        }
        node.getEdges().putAll(links);
    }

    private <T, V> TranslationLink composeLink(String word, SynsetNode<T, V> synset, List<String> translation) {
        Set<String> synsetWords = synset.getWords();
        int commonCount = 0;
        for (String t : translation) {
            if (synsetWords.contains(t)) {
                commonCount++;
            }
        }
        double weight = ((double) commonCount) / (synsetWords.size() + translation.size());
        return new TranslationLink(word, translation, weight);
    }

    private <T, V> List<SynsetNode<T, V>> findNode(NodeRepository<T, V> repo, String s) {
        SynsetNode<T, V> node = repo.getNodeById(s);
        if (node != null) {
            return Collections.singletonList(node);
        }
        return repo.findNode(new Query(s, null));
    }

    private void processParameter(String key, String value) {
        try {
            int index = key.indexOf('.');
            String prefix = key.substring(0, index);
            String rest = key.substring(index + 1);
            switch (prefix) {
                case "mp":
                    BeanUtils.getPropertyDescriptor(MetricsParams.class, rest).getWriteMethod().invoke(params, Double.parseDouble(value));
                    break;
                case "gv":
                    switch (rest) {
                        case "engine":
                            gvSettings.setEngine(value);
                            break;
                        case "reqBoth":
                            gvSettings.setRequireBoth(Boolean.parseBoolean(value));
                            break;
                        case "threshold":
                            gvSettings.setThreshold(Double.parseDouble(value));
                            break;
                    }
                    break;
                default:
                    System.err.println("Unknown key: " + key);
            }
        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
            System.err.println("Error: " + e.getClass() + " " + e.getMessage());
        }
    }

    private void printSynsets(List<SynsetNode<?, ?>> origSynsets) throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(graphvizOutFile));
             GraphVizBuilder builder = new GraphVizBuilder(gvSettings, bw)) {
            for (SynsetNode<?, ?> n : origSynsets) {
                builder.addNode(n);
            }
            List<SynsetNode<?, ?>> created = new ArrayList<>(builder.getCreated());
            Collections.sort(created, (s1, s2) -> s1.getId().compareTo(s2.getId()));
            created.stream().forEach(System.out::println);
        }
    }

}