package ru.georgeee.bachelor.yarn.db.entity;


import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Set;

@Entity
@Getter
@Setter
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "Source")
@Table(name = "Synset")
public abstract class Synset extends BaseEntity {
    @Column(nullable = false)
    private String externalId;

    public abstract Set<TranslateEdge> getTranslateEdges();

    public abstract void setTranslateEdges(Set<TranslateEdge> edges);

}
