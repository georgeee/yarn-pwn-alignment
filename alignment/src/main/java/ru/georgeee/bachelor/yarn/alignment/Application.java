package ru.georgeee.bachelor.yarn.alignment;


import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.ISynset;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.georgeee.bachelor.yarn.Yarn;
import ru.georgeee.bachelor.yarn.alignment.misc.InteractiveDictsMap;
import ru.georgeee.bachelor.yarn.alignment.misc.StageList;
import ru.georgeee.bachelor.yarn.app.MetricsParams;
import ru.georgeee.bachelor.yarn.app.PWNNodeRepository;
import ru.georgeee.bachelor.yarn.app.YarnNodeRepository;
import ru.georgeee.bachelor.yarn.core.GraphVizSettings;
import ru.georgeee.bachelor.yarn.core.SynsetNode;
import ru.georgeee.bachelor.yarn.core.TraverseSettings;
import ru.georgeee.bachelor.yarn.croudsourcing.tasks.a.Generator;
import ru.georgeee.bachelor.yarn.croudsourcing.tasks.a.Importer;
import ru.georgeee.bachelor.yarn.croudsourcing.tasks.a.MTsar;
import ru.georgeee.bachelor.yarn.db.CommonDbService;
import ru.georgeee.bachelor.yarn.db.entity.Synset;
import ru.georgeee.bachelor.yarn.db.entity.tasks.a.Worker;
import ru.georgeee.bachelor.yarn.dict.Dict;
import ru.georgeee.bachelor.yarn.dict.manipulators.StatTrackingDict;
import ru.georgeee.bachelor.yarn.xml.SynsetEntry;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootApplication(scanBasePackages = {"ru.georgeee.bachelor.yarn"})
public class Application implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(Application.class);
    @Autowired
    private MetricsParams params;
    @Autowired
    @Qualifier("appInteractiveDicts")
    private InteractiveDictsMap dicts;
    @Autowired
    @Qualifier("alignmentStages")
    private StageList stages;
    @Autowired
    private Yarn yarn;
    @Autowired
    private IDictionary pwnDict;
    private Stage interactiveStage;
    @Autowired
    private GraphVizSettings gvSettings;
    @Autowired
    private Aligner aligner;
    @Autowired
    private CommonDbService commonDbService;
    @Value("${gv.out:out.dot}")
    private String graphvizOutFile;

    @Value("${tag:}")
    private String tag;
    @Value("${action:}")
    private String action;
    @Value("${files:}")
    private String files;
    @Value("${file:}")
    private String file;
    @Value("${source:}")
    private String source;
    @Value("${author:}")
    private String author;
    @Value("${poolId:0}")
    private int poolId;
    @Value("${verbose:false}")
    private boolean verbose;

    @Autowired
    private ExportService exportService;
    @Autowired
    private Generator taskAGenerator;
    @Autowired
    private ru.georgeee.bachelor.yarn.croudsourcing.tasks.b.Importer taskBImporter;
    @Autowired
    private Importer taskAImporter;
    @Autowired
    private MTsar taskAMTsar;
    @Autowired
    private ru.georgeee.bachelor.yarn.croudsourcing.tasks.b.Generator taskBGenerator;

    @PostConstruct
    private void init() {
        setInteractiveStage(stages.get(0));
    }

    private void setInteractiveStage(Stage stage) {
        Stage copy = new Stage();
        copy.setDirectDict(stage.getDirectDict());
        copy.setReverseDict(stage.getReverseDict());
        copy.setSettings(new TraverseSettings());
        copy.getSettings().setDepth(stage.getSettings().getDepth());
        copy.getSettings().setMaxEdges(stage.getSettings().getMaxEdges());
        copy.getSettings().setThreshold(stage.getSettings().getThreshold());
        copy.getSettings().setMeanThreshold(stage.getSettings().getMeanThreshold());
        interactiveStage = copy;
    }

    public static void main(String[] args) throws Exception {
        SpringApplication application = new SpringApplication(Application.class);
        application.setApplicationContextClass(AnnotationConfigApplicationContext.class);
        SpringApplication.run(Application.class, args);
    }

    private List<String> parseIds() throws IOException {
        return Files.lines(Paths.get(file))
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toList());
    }

    @Override
    public void run(String... args) throws IOException {
        if (StringUtils.isEmpty(action)) {
            interactive();
        } else {
            switch (action) {
                case "buildGraph":
                case "bg": {
                    Set<String> orphans = aligner.alignAndExport(parseIds());
                    System.out.println("Orphans (remained synsets):");
                    orphans.forEach(System.out::println);
                    System.out.println("Verbose:");
                    orphans.forEach(this::lookupSynset);
                    break;
                }
                case "a.generate":
                case "a.gen": {
                    taskAGenerator.generateAndExportByIds(parseIds());
                    break;
                }
                case "b.generate":
                case "b.gen": {
                    taskBGenerator.generateAndExportByIds(parseIds());
                    break;
                }
                case "a.answersJson":
                case "a.aj": {
                    taskAImporter.importAnswersFromJson(Paths.get(file), Worker.Source.valueOf(source.toUpperCase()), author);
                    break;
                }
                case "a.answersToloka":
                case "a.at": {
                    taskAImporter.importAnswersFromToloka(Paths.get(file));
                    break;
                }
                case "a.aggregation":
                case "a.aggr": {
                    taskAImporter.importAggregation(Arrays.asList(files.split(","))
                            .stream().map(Paths::get)
                            .collect(Collectors.toList()));
                    break;
                }
                case "b.answersJson":
                case "b.aj": {
                    taskBImporter.importAnswersFromJson(Paths.get(file), author);
                    break;
                }
                case "a.exportMTsar":
                case "a.em": {
                    taskAMTsar.exportToMTsar(poolId);
                    break;
                }
                case "a.exportAggregationJson":
                case "a.eaj": {
                    taskAGenerator.exportAggregationsToJson(poolId);
                    break;
                }
                case "orphansInDb": {
                    List<String> synsetIds = parseIds();
                    for (Synset s : commonDbService.getOrphans(synsetIds)) {
                        if (verbose) {
                            lookupSynset(s.getExternalId());
                        } else {
                            System.out.println(s.getExternalId());
                        }
                    }
                    break;
                }
                default:
                    System.err.println("No action specified");
            }
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
            dicts.forEach((k, d) -> System.out.println(k + ": " + s + ":" + d.translate(s)));
        }
    }

    private void testAlign(String query) throws IOException {
        Aligner.Result result = aligner.align(interactiveStage, Arrays.asList(query.split("\\s*,\\s*")));
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
        for (Map.Entry<String, Dict> e : dicts.entrySet()) {
            out.append(e.getKey()).append(":\n");
            Dict dict = e.getValue();
            if (dict instanceof StatTrackingDict) {
                ((StatTrackingDict) dict).printStats(out);
            } else {
                out.append("  Stats turned off\n");
            }
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
                            interactiveStage.getSettings().setMaxEdges(Integer.parseInt(value));
                            break;
                        case "threshold":
                            interactiveStage.getSettings().setThreshold(Double.parseDouble(value));
                            break;
                        case "depth":
                            interactiveStage.getSettings().setDepth(Integer.parseInt(value));
                            break;
                        case "stage":
                            setInteractiveStage(stages.get(Integer.parseInt(value)));
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