package ru.georgeee.bachelor.yarn.core;

import java.util.*;
import java.util.function.Consumer;

public abstract class NodeRepository<T, V> {
    private final Map<String, SynsetNode<T, V>> storage = new HashMap<>();
    private int callbackCounter;
    private final Map<Integer, Consumer<SynsetNode<T, V>>> onCreateCallbacks = new HashMap<>();

    public SynsetNode<T, V> getNode(T data) {
        return getNodeById(getId(data));
    }

    public SynsetNode<T, V> getNodeById(String id) {
        if (storage.containsKey(id)) {
            return storage.get(id);
        }
        SynsetNode<T, V> newNode = createNode(id);
        if (newNode != null) {
            storage.put(id, newNode);
            onCreateCallbacks.values().stream().forEach(a -> a.accept(newNode));
        }
        return newNode;

    }

    public Runnable registerOnCreateCallback(Consumer<SynsetNode<T, V>> callback) {
        int id = ++callbackCounter;
        onCreateCallbacks.put(id, callback);
        return () -> onCreateCallbacks.remove(id);
    }

    public abstract List<SynsetNode<T, V>> findNode(Query query);

    public Collection<SynsetNode<T, V>> getNodes() {
        return storage.values();
    }

    protected abstract SynsetNode<T, V> createNode(String id);

    protected abstract String getId(T data);

    @Override
    public String toString() {
        List<SynsetNode<T, V>> list = new ArrayList<>(getNodes());
        Collections.sort(list, (l, r) -> l.getId().compareTo(r.getId()));
        return list.toString();
    }

}
