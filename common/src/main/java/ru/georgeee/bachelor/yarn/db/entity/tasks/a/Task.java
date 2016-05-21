package ru.georgeee.bachelor.yarn.db.entity.tasks.a;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Formula;
import ru.georgeee.bachelor.yarn.db.entity.BaseEntity;
import ru.georgeee.bachelor.yarn.db.entity.PwnSynset;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "CS_A_Task")
@Getter
@Setter
public class Task extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "pwnId")
    private PwnSynset pwnSynset;

    @ManyToOne(optional = false)
    @JoinColumn(name = "poolId")
    private Pool pool;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskSynset> taskSynsets = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "task")
    private List<AAnswer> answers = new ArrayList<>();

    @Formula("(SELECT COUNT(*) FROM CS_A_Answer r WHERE r.taskId = id)")
    private int resultCount;
}
