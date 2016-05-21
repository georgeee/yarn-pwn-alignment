package ru.georgeee.bachelor.yarn.db.entity.tasks.b;

import lombok.Getter;
import lombok.Setter;
import ru.georgeee.bachelor.yarn.db.entity.PwnSynset;
import ru.georgeee.bachelor.yarn.db.entity.YarnSynset;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Task implements Serializable {
    private PwnSynset pwnSynset;
    private List<YarnSynset> yarnSynsets = new ArrayList<>();
}
