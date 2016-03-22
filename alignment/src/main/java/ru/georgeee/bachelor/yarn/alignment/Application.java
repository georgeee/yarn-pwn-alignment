package ru.georgeee.bachelor.yarn.alignment;


import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.*;
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
import javax.xml.bind.JAXBException;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    @Value("${gv.out:out.dot}")
    private String graphvizOutFile;

    @Autowired
    private DictFactory dictFactory;

    @Autowired
    private MetricsParams params;

    @Autowired
    private Metrics metrics;

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

    @PostConstruct
    private void init() throws IOException, JAXBException {
        enRuDict = dictFactory.getDict(enRuDictPath);
        ruEnDict = dictFactory.getDict(ruEnDictPath);
        yarn = Yarn.create(Paths.get(yarnXmlPath));
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
                        yarnSynsets.stream().forEach(node -> metrics.processNode(ruEnDict, pwnRepo, node));
                        pwnSynsets.stream().forEach(node -> metrics.processNode(enRuDict, yarnRepo, node));
                    }
                    pwnRepo.getNodes().stream().forEach(node -> metrics.processNode(enRuDict, yarnRepo, node));
                    yarnRepo.getNodes().stream().forEach(node -> metrics.processNode(ruEnDict, pwnRepo, node));
                    printSynsets(origSynsets);
                }
            }
        }
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
                        case "th":
                            gvSettings.setThreshold(Double.parseDouble(value));
                            break;
                        case "mTh":
                            gvSettings.setMeanThreshold(Double.parseDouble(value));
                            break;
                        case "out":
                            graphvizOutFile = value;
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