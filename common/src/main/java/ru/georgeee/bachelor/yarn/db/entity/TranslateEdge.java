package ru.georgeee.bachelor.yarn.db.entity;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Getter
@Table(name = "Translate_Edge")
public class TranslateEdge implements Serializable {

    @EmbeddedId
    private TeId embeddedId = new TeId();
    @Setter
    @Basic
    @Generated(GenerationTime.INSERT)
    private Integer id;
    @ManyToOne
    @MapsId("pwnId")
    @JoinColumn(name = "pwnId")
    @Setter
    private PwnSynset pwnSynset;
    @ManyToOne
    @MapsId("yarnId")
    @JoinColumn(name = "yarnId")
    @Setter
    private YarnSynset yarnSynset;
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "masterEdgeId", referencedColumnName = "id")
    @Setter
    private TranslateEdge masterEdge;
    @Basic
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

        public TeId() {
        }

        public TeId(Integer pwnId, Integer yarnId) {
            this.pwnId = pwnId;
            this.yarnId = yarnId;
        }

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
