package ru.georgeee.bachelor.yarn.db.entity.tasks.a;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Formula;
import ru.georgeee.bachelor.yarn.db.entity.BaseEntity;
import ru.georgeee.bachelor.yarn.db.entity.YarnSynset;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "CS_A_Task_Synset")
@Getter
public class TaskSynset extends BaseEntity {
    @Setter
    @ManyToOne(optional = false)
    @JoinColumn(name = "taskId")
    private Task task;

    @Setter
    @ManyToOne
    @JoinColumn(name = "yarnId")
    private YarnSynset yarnSynset;

    @Formula("(SELECT COUNT(*) FROM CS_A_Answer r WHERE r.selectedId = id)")
    private int resultCount;
}
