package ru.georgeee.bachelor.yarn.alignment;

import java.util.Collections;
import java.util.List;

public class CompositeDict implements Dict {
    private final List<Dict> dicts;

    public CompositeDict(List<Dict> dicts) {
        this.dicts = dicts;
    }

    @Override
    public List<List<String>> translate(String word) {
        for (Dict dict : dicts) {
            List<List<String>> tr = dict.translate(word);
            if (tr != null && !tr.isEmpty()) {
                return tr;
            }
        }
        return Collections.emptyList();
    }
}
