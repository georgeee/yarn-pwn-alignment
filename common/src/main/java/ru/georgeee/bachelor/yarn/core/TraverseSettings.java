package ru.georgeee.bachelor.yarn.core;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TraverseSettings {
    private int maxEdges;
    private int depth = 2;
    private double meanThreshold;
    private double threshold;
}
