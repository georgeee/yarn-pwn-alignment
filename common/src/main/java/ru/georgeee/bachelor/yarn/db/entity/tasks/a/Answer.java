package ru.georgeee.bachelor.yarn.db.entity.tasks.a;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.georgeee.bachelor.yarn.db.entity.BaseEntity;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "CS_A_Answer")
@ToString
@Getter
@Setter
public class Answer extends BaseEntity {
    @Basic
    private Integer selectedId;

    @Column(nullable = false)
    private int taskId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "workerId")
    private Worker worker;

    @ManyToOne
    @JoinColumn(name = "taskId", insertable = false, updatable = false)
    private Task task;

    @Basic
    private String assignmentId;

    @Column(nullable = false)
    private Date createdDate;

}
