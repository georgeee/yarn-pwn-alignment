package ru.georgeee.bachelor.yarn.db.entity.tasks.a;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.georgeee.bachelor.yarn.db.entity.BaseEntity;

import javax.persistence.*;

@Entity
@Table(name = "CS_A_Worker")
@Getter
@Setter
@ToString
public class Worker extends BaseEntity{
    @Column(nullable = false)
    private String externalId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Source source;

    public enum Source {
        TOLOKA, MANUAL
    }

}
