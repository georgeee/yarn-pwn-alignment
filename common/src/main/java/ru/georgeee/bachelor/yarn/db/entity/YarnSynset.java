package ru.georgeee.bachelor.yarn.db.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@DiscriminatorValue("YARN")
@Getter
@Setter
public class YarnSynset extends Synset {
    @OneToMany(mappedBy = "yarnSynset", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<TranslateEdge> translateEdges = new HashSet<>();
}
