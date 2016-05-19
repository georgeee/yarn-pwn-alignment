package ru.georgeee.bachelor.yarn.core;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class GraphTraverser implements AutoCloseable {
    private final List<Runnable> onCreateUnregisterCallbacks = new ArrayList<>();

    private List<Pair<? extends SynsetNode<?, ?>, Consumer<TraverseSettings>>> nextNodes = new ArrayList<>();

    public <T, V> void registerRepo(NodeRepository<T, V> repo, BiFunction<SynsetNode<T, V>, TraverseSettings, ?> handler) {
        Consumer<SynsetNode<T, V>> f = n -> nextNodes.add(new ImmutablePair<>(n, s -> handler.apply(n, s)));
        onCreateUnregisterCallbacks.add(repo.registerOnCreateCallback(f));
        repo.getNodes().stream().forEach(f);
    }

    public void traverse(TraverseSettings settings) {
        for (int i = 0; i < settings.getDepth(); ++i) {
            List<Pair<? extends SynsetNode<?, ?>, Consumer<TraverseSettings>>> nextNodes = this.nextNodes;
            this.nextNodes = new ArrayList<>();
            if (i == 0)
                nextNodes.stream()
                        .forEach(p -> p.getRight().accept(settings));
            else
                nextNodes.stream()
                        .filter(p -> p.getLeft().getMaxBackEdgeWeight() >= settings.getThreshold())
                        .forEach(p -> p.getRight().accept(settings));
        }
    }

    public Set<SynsetNode<?, ?>> getRemained() {
        return nextNodes.stream().map(Pair::getLeft).collect(Collectors.toSet());
    }

    @Override
    public void close() {
        onCreateUnregisterCallbacks.forEach(Runnable::run);
    }
}
