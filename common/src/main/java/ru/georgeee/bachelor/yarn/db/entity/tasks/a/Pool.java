package ru.georgeee.bachelor.yarn.db.entity.tasks.a;

import lombok.Getter;
import lombok.Setter;
import ru.georgeee.bachelor.yarn.db.entity.BaseEntity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "CS_A_Pool")
@Getter
@Setter
public class Pool extends BaseEntity {
    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(
            name = "CS_A_Pool_Predecessor",
            joinColumns = @JoinColumn(name = "poolId"),
            inverseJoinColumns = @JoinColumn(name = "predecessorId")
    )
    private List<Pool> predecessors = new ArrayList<>();

    @Basic
    private boolean completed;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "pool", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Task> tasks = new HashSet<>();

}
