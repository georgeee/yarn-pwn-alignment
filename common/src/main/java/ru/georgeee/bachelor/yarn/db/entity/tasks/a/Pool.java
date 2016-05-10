package ru.georgeee.bachelor.yarn.db.entity.tasks.a;

import lombok.Getter;
import lombok.Setter;
import ru.georgeee.bachelor.yarn.db.entity.BaseEntity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "CS_A_Pool")
@Getter
@Setter
public class Pool extends BaseEntity {
    @OneToOne
    @JoinColumn(name = "PredecessorId")
    private Pool predecessor;

    @Basic
    private boolean completed;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "pool", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks = new ArrayList<>();

}
