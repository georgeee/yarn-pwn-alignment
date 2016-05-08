package ru.georgeee.bachelor.yarn.db.entity.tasks.a;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Formula;
import ru.georgeee.bachelor.yarn.db.entity.BaseEntity;
import ru.georgeee.bachelor.yarn.db.entity.YarnSynset;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "CS_A_Task_Synset")
@Getter
public class TaskSynset extends BaseEntity {
    @Setter
    @ManyToOne(optional = false)
    @JoinColumn(name = "taskId")
    private Task task;

    @Setter
    @ManyToOne(optional = false)
    @JoinColumn(name = "yarnId")
    private YarnSynset yarnSynset;

    @Setter
    @OneToMany(mappedBy = "taskSynset", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Result> results;

    @Formula("(SELECT COUNT(*) FROM CS_A_Result r WHERE r.taskSynsetId = id)")
    private int resultCount;
}
