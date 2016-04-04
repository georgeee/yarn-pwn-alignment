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

    @Value("${dict.stats:false}")
    private boolean dictCollectStats;

    @Autowired
    private DictFactory dictFactory;

    @Autowired
    private MetricsParams params;

    @Autowired
    private Metrics metrics;

    private Dict enRuDict;
    private Dict ruEnDict;

    private Yarn yarn;
    private IDictionary pwnDict;

    @Autowired
    private GraphSettings grSettings;

    @Autowired
    private GraphVizSettings gvSettings;

    public static void main(String[] args) throws Exception {
        SpringApplication application = new SpringApplication(Application.class);
        application.setApplicationContextClass(AnnotationConfigApplicationContext.class);
        SpringApplication.run(Application.class, args);
    }

    @PostConstruct
    private void init() throws IOException, JAXBException {
        enRuDict = initDict(enRuDictPath);
        ruEnDict = initDict(ruEnDictPath);
        yarn = Yarn.create(Paths.get(yarnXmlPath));
        pwnDict = new Dictionary(new URL("file", null, pwnHomePath));
        pwnDict.open();
    }

    private Dict initDict(String settingsString) throws IOException {
        Dict dict = dictFactory.getDict(settingsString);
        if (dictCollectStats) {
            return new StatTrackingDict(dict);
        }
        return dict;
    }

    @Override
    public void run(String... args) throws IOException, JAXBException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            String query;
            while ((query = br.readLine()) != null) {
                query = query.trim();
                if (query.isEmpty()) continue;
                if (query.charAt(0) == ':') {
                    String[] parts = query.substring(1).split("\\s+", 2);
                    String type = parts[0], q = parts.length == 2 ? parts[1] : "";
                    switch (type) {
                        case "s": {
                            int index = q.indexOf('=');
                            if (index == -1) {
                                System.err.println("Wrong format");
                            } else {
                                String key = q.substring(0, index).trim();
                                String value = q.substring(index + 1).trim();
                                processParameter(key, value);
                            }
                            break;
                        }
                        case "t": {
                            for (String s : q.split("\\s*,\\s*")) {
                                System.out.println("EnRu: " + s + ":" + enRuDict.translate(s));
                                System.out.println("RuEn: " + s + ":" + ruEnDict.translate(s));
                            }
                            break;
                        }
                        case "ds": {
                            if (q.isEmpty()) {
                                printDictStats(System.out);
                            } else {
                                try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(q))) {
                                    printDictStats(bw);
                                }
                            }
                            break;
                        }
                        default:
                            System.err.println("Unknown query: " + query);
                    }
                } else {
                    PWNNodeRepository<SynsetEntry> pwnRepo = new PWNNodeRepository<>(pwnDict);
                    YarnNodeRepository<ISynset> yarnRepo = new YarnNodeRepository<>(yarn);
                    GraphTraverser traverser = new GraphTraverser();
                    traverser.registerRepo(pwnRepo, (n, s) -> {
                        metrics.processNode(s, enRuDict, yarnRepo, n);
                        return null;
                    });
                    traverser.registerRepo(yarnRepo, (n, s) -> {
                        metrics.processNode(s, ruEnDict, pwnRepo, n);
                        return null;
                    });
                    for (String s : query.split("\\s*,\\s*")) {
                        findNode(yarnRepo, s);
                        findNode(pwnRepo, s);
                    }
                    traverser.traverse(grSettings);
                    try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(graphvizOutFile));
                         GraphVizBuilder builder = new GraphVizBuilder(gvSettings, bw)) {
                        builder.addIgnored(traverser.getRemained());
                        builder.addRepo(pwnRepo);
                        builder.addRepo(yarnRepo);
                        List<SynsetNode<?, ?>> created = new ArrayList<>(builder.getCreated());
                        Collections.sort(created, (s1, s2) -> s1.getId().compareTo(s2.getId()));
                        created.stream().forEach(System.out::println);
                    }
                }
            }
        }
    }

    private void printDictStats(Appendable out) throws IOException {
        out.append("En-ru dict:\n");
        if (enRuDict instanceof StatTrackingDict) {
            ((StatTrackingDict) enRuDict).printStats(out);
        } else {
            out.append("  Stats turned off\n");
        }
        out.append("Ru-en dict:\n");
        if (ruEnDict instanceof StatTrackingDict) {
            ((StatTrackingDict) ruEnDict).printStats(out);
        } else {
            out.append("  Stats turned off\n");
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
                case "gr":
                    switch (rest) {
                        case "maxEdges":
                            grSettings.setMaxEdges(Integer.parseInt(value));
                            break;
                        case "threshold":
                            grSettings.setThreshold(Double.parseDouble(value));
                            break;
                        case "depth":
                            grSettings.setDepth(Integer.parseInt(value));
                            break;
                        default:
                            System.err.println("Unknown key: " + key);
                    }
                    break;
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
                        default:
                            System.err.println("Unknown key: " + key);
                    }
                    break;
                default:
                    System.err.println("Unknown key: " + key);
            }
        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
            System.err.println("Error: " + e.getClass() + " " + e.getMessage());
        }
    }

}