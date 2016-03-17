package ru.georgeee.bachelor.yarn.graph;

import frege.runtime.Delayed;
import frege.runtime.Phantom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.georgeee.bachelor.yarn.alignment.MetricsParams;
import ru.georgeee.bachelor.yarn.alignment.SimpleDict;

@Component
public class FregeHelper {

    @Autowired
    private MetricsParams metricsParams;

    public <T, V> void processNode(SimpleDict dict, NodeRepository<V, T> repo, SynsetNode<T, V> node) {
        Delayed.forced(Metrics.processNode(metricsParams, dict, repo, node).apply(Phantom.theRealWorld));
    }
}
