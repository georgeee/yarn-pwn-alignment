package ru.georgeee.bachelor.yarn.alignment;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import ru.georgeee.bachelor.yarn.graph.NodeRepository;
import ru.georgeee.bachelor.yarn.graph.SynsetNode;
import ru.georgeee.bachelor.yarn.graph.GraphSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class GraphTraverser implements AutoCloseable {
    private final List<Runnable> onCreateUnregisterCallbacks = new ArrayList<>();

    private List<Pair<? extends SynsetNode<?, ?>, Consumer<GraphSettings>>> nextNodes = new ArrayList<>();

    public <T, V> void registerRepo(NodeRepository<T, V> repo, BiFunction<SynsetNode<T, V>, GraphSettings, ?> handler) {
        Consumer<SynsetNode<T, V>> f = n -> nextNodes.add(new ImmutablePair<>(n, s -> handler.apply(n, s)));
        onCreateUnregisterCallbacks.add(repo.registerOnCreateCallback(f));
        repo.getNodes().stream().forEach(f);
    }

    public void traverse(GraphSettings settings) {
        for (int i = 0; i < settings.getDepth(); ++i) {
            List<Pair<? extends SynsetNode<?, ?>, Consumer<GraphSettings>>> nextNodes = this.nextNodes;
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
