package ru.georgeee.bachelor.yarn.db.entity.tasks.a;

import lombok.Getter;
import lombok.Setter;
import ru.georgeee.bachelor.yarn.db.entity.BaseEntity;

import javax.persistence.*;

@Entity
@Table(name = "CS_A_Result")
@Getter
@Setter
public class Result extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "selectedId")
    private TaskSynset taskSynset;

    @Basic
    private String worker;

    @Basic
    private String extrenalId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Source source;

    public enum Source {
        TOLOKA
    }
}
