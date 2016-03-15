package ru.georgeee.bachelor.yarn.alignment;

import java.util.List;

public interface SimpleDict {
    List<List<String>> translate(String word);

    default String[][] translateAsArray(String word) {
        List<List<String>> res = translate(word);
        String[][] res2 = new String[res.size()][];
        for (int i = 0; i < res.size(); ++i) {
            List<String> l = res.get(i);
            res2[i] = l.toArray(new String[l.size()]);
        }
        return res2;
    }
}
