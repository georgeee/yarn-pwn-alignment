package ru.georgeee.bachelor.yarn.alignment;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.analysis.FunctionUtils;
import ru.georgeee.bachelor.yarn.graph.NodeRepository;
import ru.georgeee.bachelor.yarn.graph.SynsetNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RunnableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class GraphTraverser implements AutoCloseable {
    private final List<Runnable> onCreateUnregisterCallbacks = new ArrayList<>();

    private List<Pair<? extends SynsetNode<?, ?>, Runnable>> nextNodes = new ArrayList<>();

    public <T, V> void registerRepo(NodeRepository<T, V> repo, Consumer<SynsetNode<T, V>> handler) {
        Consumer<SynsetNode<T, V>> f = n -> nextNodes.add(new ImmutablePair<>(n, () -> handler.accept(n)));
        onCreateUnregisterCallbacks.add(repo.registerOnCreateCallback(f));
        repo.getNodes().stream().forEach(f);
    }

    public void traverse(int depth) {
        for (int i = 0; i < depth; ++i) {
            List<Pair<? extends SynsetNode<?, ?>, Runnable>> nextNodes = this.nextNodes;
            this.nextNodes = new ArrayList<>();
            nextNodes.stream().forEach(p -> p.getRight().run());
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
