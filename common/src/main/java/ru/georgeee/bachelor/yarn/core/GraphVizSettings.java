package ru.georgeee.bachelor.yarn.core;

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

    @Value("${gv.threshold:0.01}")
    private double threshold;

    @Value("${gv.mThreshold:0.01}")
    private double meanThreshold;

    @Value("${gv.maxEdges:0}")
    private int maxEdges;
}
