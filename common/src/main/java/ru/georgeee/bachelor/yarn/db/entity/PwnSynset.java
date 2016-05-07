package ru.georgeee.bachelor.yarn.db.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@DiscriminatorValue("PWN")
@Getter
@Setter
public class PwnSynset extends Synset {
    @OneToMany(mappedBy = "pwnSynset", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<TranslateEdge> translateEdges = new HashSet<>();
}
