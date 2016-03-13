package ru.georgeee.bachelor.yarn.graph;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Node <T, V>{
    private final List<Edge> edges = new ArrayList<>();
    private final T data;

    public Node(T data) {
        this.data = data;
    }

    @Getter
    public class Edge{
        private Node<V, T> dest;
        private double weight;
    }
}
