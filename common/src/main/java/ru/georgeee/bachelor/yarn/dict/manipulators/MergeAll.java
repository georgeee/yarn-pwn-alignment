package ru.georgeee.bachelor.yarn.dict.manipulators;

import ru.georgeee.bachelor.yarn.dict.Dict;

import java.util.ArrayList;
import java.util.List;

public class MergeAll implements Dict {
    private final List<Dict> dicts;

    public MergeAll(List<Dict> dicts) {
        this.dicts = dicts;
    }

    @Override
    public List<Translation> translate(String word) {
        List<Translation> translations = new ArrayList<>();
        for (Dict dict : dicts) {
            List<Translation> tr = dict.translate(word);
            if (tr != null) {
                translations.addAll(tr);
            }
        }
        return translations;
    }
}
