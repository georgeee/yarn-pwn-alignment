package ru.georgeee.bachelor.yarn.db.entity.tasks.b;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.georgeee.bachelor.yarn.db.entity.BaseEntity;
import ru.georgeee.bachelor.yarn.db.entity.PwnSynset;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "CS_B_Answer")
@ToString
@Getter
@Setter
public class BAnswer extends BaseEntity {
    @Basic
    private String worker;
    @Basic
    private int pwnId;

    @ManyToOne
    @JoinColumn(name = "pwnId", insertable = false, updatable = false)
    private PwnSynset pwnSynset;

    @Basic
    private String assignmentId;

    @Column(nullable = false)
    private Date createdDate;

    @OneToMany(mappedBy = "answer", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AnswerSelected> selectedList = new ArrayList<>();
}
