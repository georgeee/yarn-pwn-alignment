package ru.georgeee.bachelor.yarn.clustering;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.georgeee.bachelor.yarn.core.POS;
import ru.georgeee.bachelor.yarn.core.SynsetNode;
import ru.georgeee.bachelor.yarn.core.TranslationLink;
import ru.georgeee.bachelor.yarn.core.WordData;

import java.util.*;

public class Cluster<T, V> extends SynsetNode<T, V> implements Comparable<Cluster<T, V>>, Iterable<Cluster.Member<T, V>> {
    private static final Logger log = LoggerFactory.getLogger(Clusterer.class);
    private final String id;
    private final TreeSet<Member<T, V>> members = new TreeSet<>();

    public Cluster(SynsetNode<V, T> srcNode, Member<T, V> first) {
        id = "CL-" + first.getNode().getId();
        members.add(first);
        getEdges().put(srcNode, new TranslationLink(first.getRWeight()));
    }

    public Member<T, V> first() {
        return members.first();
    }

    public int size() {
        return members.size();
    }

    public void addMember(Member<T, V> member) {
        members.add(member);
        setMaxBackEdgeWeight(Math.max(getMaxBackEdgeWeight(), member.getNode().getMaxBackEdgeWeight()));
        setBackEdgeCount(getBackEdgeCount() + member.getNode().getBackEdgeCount());
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Set<String> getWords() {
        return members.first().getNode().getWords();
    }

    @Override
    public List<WordData> getWordsWithData() {
        return members.first().getNode().getWordsWithData();
    }

    @Override
    public POS getPOS() {
        return members.first().getNode().getPOS();
    }

    @Override
    public int compareTo(Cluster<T, V> o) {
        return members.first().compareTo(o.members.first());
    }

    @Override
    public Iterator<Member<T, V>> iterator() {
        return members.iterator();
    }

    @Override
    public String toString() {
        return "Cluster{" +
                "id='" + id + '\'' +
                ", members=" + members +
                '}';
    }

    public Set<Member<T, V>> getUnderlyingNodes() {
        return Collections.unmodifiableSet(members);
    }

    @Getter
    public static class Member<T, V> implements Comparable<Member> {
        private final SynsetNode<T, V> node;
        private final TranslationLink link;
        private final TranslationLink rLink;

        public Member(SynsetNode<T, V> node, TranslationLink link, TranslationLink rLink) {
            this.node = node;
            this.link = link;
            this.rLink = rLink;
        }

        public double getRWeight() {
            return rLink == null ? 0 : rLink.getWeight();
        }

        public double getWeight() {
            return link == null ? 0 : link.getWeight();
        }

        public double getMWeight() {
            return (getWeight() + getRWeight()) / 2;
        }

        @Override
        public int compareTo(Member o) {
            double l = getMWeight();
            double r = o.getMWeight();
            return l == r ? node.getId().compareTo(o.getNode().getId()) : -Double.compare(l, r);
        }

        @Override
        public String toString() {
            return node.getId();
        }
    }
}
