package ru.georgeee.bachelor.yarn.db.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "Synset_Image")
@Getter
@Setter
public class SynsetImage extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "synsetId")
    private Synset synset;

    @Column(nullable = false)
    private String filename;

    @Basic
    private Integer priority;

    @Basic
    @Enumerated(EnumType.STRING)
    private Source source;

    public enum Source {
        IMAGENET
    }
}
