package ru.georgeee.bachelor.yarn.graph;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class GraphVizSettings {
    @Value("${gv.engine:fdp}")
    private String engine;

    @Value("${gv.threshold:0.51}")
    private double threshold;
}
