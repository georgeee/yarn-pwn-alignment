package ru.georgeee.bachelor.yarn.alignment;


import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.georgeee.bachelor.yarn.Yarn;
import ru.georgeee.bachelor.yarn.app.*;
import ru.georgeee.bachelor.yarn.clustering.Clusterer;
import ru.georgeee.bachelor.yarn.dict.Dict;
import ru.georgeee.bachelor.yarn.core.*;
import ru.georgeee.bachelor.yarn.dict.StatTrackingDict;
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

    @Value("${gv.out:out.dot}")
    private String graphvizOutFile;

    @Autowired
    private MetricsParams params;

    @Autowired
    private Metrics metrics;

    @Autowired
    @Qualifier("enRuDict")
    private Dict enRuDict;

    @Autowired
    @Qualifier("ruEnDict")
    private Dict ruEnDict;

    private Yarn yarn;
    private IDictionary pwnDict;

    @Autowired
    private GraphSettings grSettings;

    @Autowired
    private GraphVizSettings gvSettings;

    @Autowired
    private Clusterer clusterer;

    @Autowired
    private ApplicationContext context;

    public static void main(String[] args) throws Exception {
        SpringApplication application = new SpringApplication(Application.class);
        application.setApplicationContextClass(AnnotationConfigApplicationContext.class);
        SpringApplication.run(Application.class, args);
    }

    @PostConstruct
    private void init() throws IOException, JAXBException {
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
                    String[] parts = query.substring(1).split("\\s+", 2);
                    String type = parts[0], q = parts.length == 2 ? parts[1].trim() : "";
                    switch (type) {
                        case "w": {
                            PWNNodeRepository<SynsetEntry> pwnRepo = new PWNNodeRepository<>(pwnDict);
                            YarnNodeRepository<ISynset> yarnRepo = new YarnNodeRepository<>(yarn);
                            for (String s : q.split("\\s*,\\s*")) {
                                System.out.println(s);
                                SynsetNode<SynsetEntry, ISynset> yarnNode = yarnRepo.getNodeById(s);
                                SynsetNode<ISynset, SynsetEntry> pwnNode = pwnRepo.getNodeById(s);
                                System.out.println("\tYarn: " + yarnNode);
                                System.out.println("\tPWN: " + pwnNode);
                            }
                            break;
                        }
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
                    clusterer.clusterizeOutgoingEdges(pwnRepo);
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
                        case "maxEdges":
                            gvSettings.setMaxEdges(Integer.parseInt(value));
                            break;
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