package ru.georgeee.bachelor.yarn.alignment;


import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.*;
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

    @Value("${out.dot:out.dot}")
    private String graphvizOutFile;

    @Autowired
    private DictFactory dictFactory;

    @Autowired
    private FregeHelper fregeHelper;

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
            String s;
            while ((s = br.readLine()) != null && !s.isEmpty()) {
                s = s.trim();
                if (s.charAt(0) == ':') {
                    int index = s.indexOf('=');
                    if (index == -1) {
                        System.err.println("Wrong format");
                    } else {
                        String key = s.substring(1, index).trim();
                        String value = s.substring(index + 1).trim();
                        processParameter(key, value);
                    }
                } else {
                    System.out.println("EnRu: " + s + ":" + enRuDict.translate(s));
                    System.out.println("RuEn: " + s + ":" + ruEnDict.translate(s));
                    testOne(s);
                }
            }
        }
    }

    private void processParameter(String key, String value) {
        try {
            switch (key) {
                case "p1.mean":
                    params.setP1Mean(Double.parseDouble(value));
                    break;
                case "p1.sd":
                    params.setP1Sd(Double.parseDouble(value));
                    break;
                case "p2.sd":
                    params.setP2Sd(Double.parseDouble(value));
                    break;
                case "gv.engine":
                    gvSettings.setEngine(value);
                    break;
                case "gv.threshold":
                    gvSettings.setThreshold(Double.parseDouble(value));
                    break;
                default:
                    System.err.println("Unknown key: " + key);
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getClass() + " " + e.getMessage());
        }
    }

    private void testOne(String s) throws IOException {
        PWNNodeRepository<SynsetEntry> pwnRepo = new PWNNodeRepository<>(pwnDict);
        YarnNodeRepository<ISynset> yarnRepo = new YarnNodeRepository<>(yarn);
        List<SynsetNode<SynsetEntry, ISynset>> yarnSynsets = yarnRepo.findNode(new Query(s, null));
        List<SynsetNode<ISynset, SynsetEntry>> pwnSynsets = pwnRepo.findNode(new Query(s, null));
        List<SynsetNode<?, ?>> origSynsets = new ArrayList<>();
        origSynsets.addAll(yarnSynsets);
        origSynsets.addAll(pwnSynsets);
        yarnSynsets.stream().forEach(node -> fregeHelper.processNode(ruEnDict, pwnRepo, node));
        pwnSynsets.stream().forEach(node -> fregeHelper.processNode(enRuDict, yarnRepo, node));
        pwnRepo.getNodes().stream().forEach(node -> fregeHelper.processNode(enRuDict, yarnRepo, node));
        yarnRepo.getNodes().stream().forEach(node -> fregeHelper.processNode(ruEnDict, pwnRepo, node));
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

    private <T, V> void printRepo(NodeRepository<T, V> repo) {
        for (SynsetNode<T, V> node : repo.getNodes()) {
            System.out.println("Node " + node);
        }
    }

    public void testDictionary() throws IOException {
        IIndexWord idxWord = pwnDict.getIndexWord("dog", POS.NOUN);
        for (IWordID wordID : idxWord.getWordIDs()) {
            IWord word = pwnDict.getWord(wordID);
            System.out.println("Id = " + wordID);
            System.out.println("Lemma = " + word.getLemma());
            System.out.println("Synset = " + word.getSynset().getWords());
            System.out.println("Gloss = " + word.getSynset().getGloss());
        }
    }
}