package ru.georgeee.bachelor.yarn.alignment;


import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.ISynset;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.georgeee.bachelor.yarn.Yarn;
import ru.georgeee.bachelor.yarn.app.MetricsParams;
import ru.georgeee.bachelor.yarn.app.PWNNodeRepository;
import ru.georgeee.bachelor.yarn.app.YarnNodeRepository;
import ru.georgeee.bachelor.yarn.core.GraphSettings;
import ru.georgeee.bachelor.yarn.core.GraphVizSettings;
import ru.georgeee.bachelor.yarn.core.SynsetNode;
import ru.georgeee.bachelor.yarn.dict.Dict;
import ru.georgeee.bachelor.yarn.dict.StatTrackingDict;
import ru.georgeee.bachelor.yarn.xml.SynsetEntry;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootApplication(scanBasePackages = {"ru.georgeee.bachelor.yarn"})
public class Application implements CommandLineRunner {

    @Autowired
    private MetricsParams params;

    @Autowired
    @Qualifier("enRuDict")
    private Dict enRuDict;

    @Autowired
    @Qualifier("ruEnDict")
    private Dict ruEnDict;

    @Autowired
    private Yarn yarn;
    @Autowired
    private IDictionary pwnDict;

    @Autowired
    private GraphSettings grSettings;

    @Autowired
    private GraphVizSettings gvSettings;

    @Autowired
    private Aligner aligner;

    @Autowired
    private ApplicationContext context;

    @Value("${gv.out:out.dot}")
    private String graphvizOutFile;

    @Value("${app.export.db:true}")
    private boolean exportToDb;

    @Value("${idsFile:}")
    private String idsFile;

    @Autowired
    private ExportService exportService;


    public static void main(String[] args) throws Exception {
        SpringApplication application = new SpringApplication(Application.class);
        application.setApplicationContextClass(AnnotationConfigApplicationContext.class);
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) throws IOException {
        if (StringUtils.isEmpty(idsFile)) {
            interactive();
        } else {
            List<String> ids = Files.lines(Paths.get(idsFile))
                    .map(String::trim)
                    .filter(StringUtils::isNotEmpty)
                    .collect(Collectors.toList());
            exportService.exportToDb(aligner.align(ids));
        }
    }

    private void lookupSynset(String q) {
        PWNNodeRepository<SynsetEntry> pwnRepo = new PWNNodeRepository<>(pwnDict);
        YarnNodeRepository<ISynset> yarnRepo = new YarnNodeRepository<>(yarn);
        for (String s : q.split("\\s*,\\s*")) {
            System.out.println(s);
            SynsetNode<SynsetEntry, ISynset> yarnNode = yarnRepo.getNodeById(s);
            SynsetNode<ISynset, SynsetEntry> pwnNode = pwnRepo.getNodeById(s);
            System.out.println("\tYarn: " + yarnNode);
            System.out.println("\tPWN: " + pwnNode);
        }
    }

    private void updateSetting(String q) {
        int index = q.indexOf('=');
        if (index == -1) {
            System.err.println("Wrong format");
        } else {
            String key = q.substring(0, index).trim();
            String value = q.substring(index + 1).trim();
            processParameter(key, value);
        }
    }

    private void lookupWordTranslations(String q) {
        for (String s : q.split("\\s*,\\s*")) {
            System.out.println("EnRu: " + s + ":" + enRuDict.translate(s));
            System.out.println("RuEn: " + s + ":" + ruEnDict.translate(s));
        }
    }

    private void testAlign(String query) throws IOException {
        Aligner.Result result = aligner.align(Arrays.asList(query.split("\\s*,\\s*")));
        if (exportToDb) {
            exportService.exportToDb(result);
        }
        if (StringUtils.isNotEmpty(graphvizOutFile)) {
            exportService.exportToGraphViz(Paths.get(graphvizOutFile), result);
        }
    }

    private void interactive() throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            String query;
            while ((query = br.readLine()) != null) {
                query = query.trim();
                if (query.isEmpty()) continue;
                if (query.charAt(0) == ':') {
                    String[] parts = query.substring(1).split("\\s+", 2);
                    String type = parts[0], q = parts.length == 2 ? parts[1].trim() : "";
                    switch (type) {
                        case "w":
                            lookupSynset(q);
                            break;
                        case "s":
                            updateSetting(q);
                            break;
                        case "t":
                            lookupWordTranslations(q);
                            break;
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
                    testAlign(query);
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