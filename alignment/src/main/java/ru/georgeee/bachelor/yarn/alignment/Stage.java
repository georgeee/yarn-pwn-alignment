package ru.georgeee.bachelor.yarn.alignment;

import lombok.Getter;
import lombok.Setter;
import ru.georgeee.bachelor.yarn.core.TraverseSettings;
import ru.georgeee.bachelor.yarn.dict.Dict;

@Getter @Setter
public class Stage {
    private Dict directDict;
    private Dict reverseDict;
    private TraverseSettings settings;
}
