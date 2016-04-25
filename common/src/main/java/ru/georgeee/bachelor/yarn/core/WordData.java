package ru.georgeee.bachelor.yarn.core;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Getter
@Setter
public class WordData {
    private String lemma;
    private List<String> glosses;
    private List<String> examples;

    @Override
    public String toString() {
        return !CollectionUtils.isEmpty(glosses) || !CollectionUtils.isEmpty(examples)
                ? "{" + lemma + " glosses=" + glosses + ", ex=" + examples + '}'
                : lemma;
    }
}
