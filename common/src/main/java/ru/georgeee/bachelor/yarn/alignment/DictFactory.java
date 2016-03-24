package ru.georgeee.bachelor.yarn.alignment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.georgeee.bachelor.yarn.dict.ya.LookupResponse;
import ru.georgeee.bachelor.yarn.dict.ya.YandexDictionary;
import ru.georgeee.stardict.Stardict;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DictFactory {
    private static final Logger log = LoggerFactory.getLogger(DictFactory.class);
    @Value("${dict.ya.apiKey:}")
    private String yaApiKey;

    public Dict getDict(String paths) throws IOException {
        String[] pathsParts = paths.split(";");
        List<Dict> dicts = new ArrayList<>();
        for (String _path : pathsParts) {
            String[] _pathParts = _path.split(":", 2);
            String type = _pathParts[0];
            String path = _pathParts.length == 2 ? _pathParts[1] : null;
            switch (type) {
                case "stardict": {
                    if (path == null) {
                        log.error("Wrong path: {}", _path);
                        break;
                    }
                    String[] parts = path.split(":", 2);
                    Stardict stardict = Stardict.createRAM(Paths.get(parts[0]), Paths.get(parts[1]));
                    dicts.add(word -> {
                        Stardict.WordPosition wordPos = stardict.getWords().get(word);
                        if (wordPos == null) {
                            return Collections.emptyList();
                        }
                        return wordPos.getTranslations();
                    });
                    break;
                }
                case "ya":
                case "yandex": {
                    if (path == null) {
                        log.error("Wrong path: {}", _path);
                        break;
                    }
                    YandexDictionary yaDict = new YandexDictionary(yaApiKey);
                    dicts.add(word -> {
                        try {
                            LookupResponse response = yaDict.translate(word, path);
                            return response.getDef().stream()
                                    .flatMap(def -> def.getTranslations()
                                            .stream()
                                            .map(tr -> {
                                                List<String> res = new ArrayList<>();
                                                res.add(tr.getText());
                                                if (tr.getSynonyms() != null) {
                                                    tr.getSynonyms().stream().map(LookupResponse.Word::getText).forEach(res::add);
                                                }
                                                return res;
                                            }))
                                    .collect(Collectors.toList());
                        } catch (Exception e) {
                            log.warn("Error communicating to slovari.yandex", e);
                            return Collections.emptyList();
                        }
                    });
                    break;
                }
                default:
                    log.error("Can't parse path: {}", _path);
            }
        }
        return new CompositeDict(dicts);
    }

}
