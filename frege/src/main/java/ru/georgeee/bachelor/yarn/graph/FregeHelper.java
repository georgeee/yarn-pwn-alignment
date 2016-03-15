package ru.georgeee.bachelor.yarn.graph;

import frege.runtime.Delayed;
import frege.runtime.Phantom;
import ru.georgeee.bachelor.yarn.alignment.SimpleDict;


public class FregeHelper {
    public static <T, V> void processNode(SimpleDict dict, NodeRepository<V, T> repo, SynsetNode<T, V> node) {
        Delayed.forced(Metrics.processNode(dict, repo, node).apply(Phantom.theRealWorld));
    }

    public static <T, V> void test(SimpleDict dict, NodeRepository<V, T> repo, SynsetNode<T, V> node) {
        Object result = Delayed.forced(Metrics.test(dict, repo, node).apply(Phantom.theRealWorld));
        System.out.println(result.toString());
//        return Delayed.forced(Metrics.test(node));
    }
}
