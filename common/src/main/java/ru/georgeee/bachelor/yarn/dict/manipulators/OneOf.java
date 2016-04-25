package ru.georgeee.bachelor.yarn.dict.manipulators;

import ru.georgeee.bachelor.yarn.dict.Dict;

import java.util.Collections;
import java.util.List;

public class OneOf implements Dict {
    private final List<Dict> dicts;

    public OneOf(List<Dict> dicts) {
        this.dicts = dicts;
    }

    @Override
    public List<Translation> translate(String word) {
        for (Dict dict : dicts) {
            List<Translation> tr = dict.translate(word);
            if (tr != null && !tr.isEmpty()) {
                return tr;
            }
        }
        return Collections.emptyList();
    }
}
