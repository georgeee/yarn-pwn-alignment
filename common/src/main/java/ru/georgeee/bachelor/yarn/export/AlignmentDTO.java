package ru.georgeee.bachelor.yarn.export;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;

public class AlignmentDTO extends HashMap<String, List<AlignmentDTO.Edge>> {
    @Getter
    @Setter
    public static class Edge {
        private SubEdge representative;
        private List<SubEdge> hidden;
    }

    @Getter
    @Setter
    public static class SubEdge {
        private double weight;
        private double rWeight;
        private String id;
    }
}
