package ru.georgeee.bachelor.yarn.db.entity.tasks.a;

import lombok.Getter;
import lombok.Setter;
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
    @ManyToOne(optional = false)
    @JoinColumn(name = "pwnId")
    private PwnSynset pwnSynset;

    @ManyToOne(optional = false)
    @JoinColumn(name = "poolId")
    private Pool pool;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskSynset> taskSynsets = new ArrayList<>();
}
