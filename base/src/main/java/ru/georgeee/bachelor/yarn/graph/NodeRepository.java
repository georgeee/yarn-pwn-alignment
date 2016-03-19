package ru.georgeee.bachelor.yarn.graph;

import java.util.*;

public abstract class NodeRepository<T, V> {
    private final Map<String, SynsetNode<T, V>> storage = new HashMap<>();

    public SynsetNode<T, V> getNode(T data) {
        String id = getId(data);
        return storage.computeIfAbsent(id, _id -> createNode(data));
    }

    public abstract List<SynsetNode<T, V>> findNode(Query query);

    public Collection<SynsetNode<T, V>> getNodes() {
        return storage.values();
    }

    protected abstract SynsetNode<T, V> createNode(T data);

    protected abstract String getId(T data);

    @Override
    public String toString() {
        List<SynsetNode<T, V>> list = new ArrayList<>(getNodes());
        Collections.sort(list, (l, r) -> l.getId().compareTo(r.getId()));
        return list.toString();
    }
}
