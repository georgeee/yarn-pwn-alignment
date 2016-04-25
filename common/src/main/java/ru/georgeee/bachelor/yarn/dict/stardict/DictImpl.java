package ru.georgeee.bachelor.yarn.dict.stardict;

import ru.georgeee.bachelor.yarn.dict.Dict;
import ru.georgeee.stardict.Stardict;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DictImpl implements Dict {
    private final Stardict stardict;

    public DictImpl(String idxPath, String dictPath) throws IOException {
        this(Paths.get(idxPath), Paths.get(dictPath));
    }

    public DictImpl(Path idxPath, Path dictPath) throws IOException {
        stardict = Stardict.createRAF(idxPath, dictPath);
    }

    @Override
    public List<Translation> translate(String word) {
        Stardict.WordPosition wordPos = stardict.getWords().get(word);
        if (wordPos == null) {
            return Collections.emptyList();
        }
        return wordPos.getTranslations().stream()
                .map(Dict.Translation::new)
                .collect(Collectors.toList());
    }
}
