package ru.georgeee.bachelor.yarn.graph;

import lombok.Getter;

import java.util.List;

@Getter
public class Node <T, V>{
    private List<Edge> edges;
    private T data;

    @Getter
    public class Edge{
        private Node<V, T> dest;
        private double weight;
    }
}
