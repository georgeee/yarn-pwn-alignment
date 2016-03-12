package ru.georgeee.bachelor.yarn.alignment;


import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.georgeee.SimpleDict;
import ru.georgeee.bachelor.yarn.Yarn;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@SpringBootApplication
public class Application implements CommandLineRunner {

    @Value("${pwn.home}")
    private String pwnHomePath;

    @Value("${yarn.xml}")
    private String yarnXmlPath;

    @Value("${dict.en_ru}")
    private String enRuDictPath;

    @Value("${dict.ru_en}")
    private String ruEnDictPath;

    @Autowired
    private DictFactory dictFactory;

    private SimpleDict enRuDict;
    private SimpleDict ruEnDict;

    private Yarn yarn;
    private IDictionary pwnDict;

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
                System.out.println("EnRu: " + s + ":" + enRuDict.translate(s));
                System.out.println("RuEn: " + s + ":" + ruEnDict.translate(s));
                testYarn2PWN(s);
                testPWN2Yarn(s);
            }
        }
    }

    private void testPWN2Yarn(String s) {

    }

    private void testYarn2PWN(String s) {
        List<Yarn.Word> wordDefs = yarn.getWord(s);
        for(Yarn.Word word : wordDefs){
            System.out.println("### Definition " + word.getId());
            for(Yarn.WordSynsetEntry entry : word.getSynsets()){
                System.out.println("## Synset entry " + entry);
            }
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