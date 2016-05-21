package ru.georgeee.bachelor.yarn.db.entity.tasks.a;

import lombok.Getter;
import lombok.Setter;
import ru.georgeee.bachelor.yarn.db.entity.BaseEntity;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Getter
@Setter
@Table(name = "CS_A_Aggregation")
public class Aggregation extends BaseEntity {

    @Basic
    private double weight;

    @Basic
    private String tag;

    @Basic
    private Integer taskId;

    @Basic
    private Integer selectedId;
}
