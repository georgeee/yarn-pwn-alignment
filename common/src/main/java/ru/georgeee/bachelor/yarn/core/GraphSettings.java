package ru.georgeee.bachelor.yarn.core;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter @Setter
public class GraphSettings {
    @Value("${gr.maxEdges:0}")
    private int maxEdges;
    @Value("${gr.depth:2}")
    private int depth;
    @Value("${gr.threshold:0.01}")
    private double threshold;
}
