package ru.georgeee.bachelor.yarn.db.entity;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "Translate_Edge")
public class TranslateEdge implements Serializable {

    @Getter
    @EmbeddedId
    private TeId embeddedId = new TeId();
    @Getter
    @Setter
    @Basic
    @Generated(GenerationTime.INSERT)
    private Integer id;
    @ManyToOne
    @MapsId("pwnId")
    @JoinColumn(name = "pwnId")
    @Getter
    @Setter
    private PwnSynset pwnSynset;
    @ManyToOne
    @MapsId("yarnId")
    @JoinColumn(name = "yarnId")
    @Getter
    @Setter
    private YarnSynset yarnSynset;
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "masterEdgeId", referencedColumnName = "id")
    @Getter
    @Setter
    private TranslateEdge masterEdge;
    @Basic
    @Getter
    @Setter
    private double weight;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TranslateEdge edge = (TranslateEdge) o;
        return Objects.equals(pwnSynset, edge.pwnSynset) &&
                Objects.equals(yarnSynset, edge.yarnSynset);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pwnSynset, yarnSynset);
    }

    @Embeddable
    @Getter
    @Setter
    public static class TeId implements Serializable {
        private Integer pwnId;
        private Integer yarnId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TeId id = (TeId) o;
            return Objects.equals(pwnId, id.pwnId) &&
                    Objects.equals(yarnId, id.yarnId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pwnId, yarnId);
        }
    }
}
