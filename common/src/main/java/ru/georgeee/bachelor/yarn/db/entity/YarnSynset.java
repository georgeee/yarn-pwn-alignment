package ru.georgeee.bachelor.yarn.db.entity;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("YARN")
@Getter
public class YarnSynset extends Synset {
    @OneToMany(mappedBy = "yarnSynset", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Setter
    private List<TranslateEdge> translateEdges = new ArrayList<>();

    @Formula("(SELECT COUNT(*) FROM Translate_Edge e WHERE e.pwnId = id)")
    private int edgeCount;

    @OneToMany(mappedBy = "yarnSynset", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy(value = "weight DESC")
    @Where(clause = "masterEdgeId IS NULL")
    private List<TranslateEdge> notMasteredTranslateEdges = new ArrayList<>();
}
