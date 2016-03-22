package ru.georgeee.bachelor.yarn.alignment;

import org.springframework.stereotype.Component;
import ru.georgeee.stardict.Stardict;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;

@Component
public class DictFactory {
    public SimpleDict getDict(String path) throws IOException {
        if(path.startsWith("stardict:")){
            String [] parts = path.split(":");
            Stardict stardict = Stardict.createRAM(Paths.get(parts[1]), Paths.get(parts[2]));
            return word -> {
                Stardict.WordPosition wordPos = stardict.getWords().get(word);
                if(wordPos == null){
                    return Collections.emptyList();
                }
                return wordPos.getTranslations();
            };
        }
        throw new IllegalArgumentException("Can't parse path: " + path);
    }
}
