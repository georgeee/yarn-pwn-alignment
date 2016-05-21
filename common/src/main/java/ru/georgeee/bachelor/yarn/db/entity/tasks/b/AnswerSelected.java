package ru.georgeee.bachelor.yarn.db.entity.tasks.b;


import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Getter
@Table(name = "CS_B_Answer_Selected")
public class AnswerSelected implements Serializable {
    @EmbeddedId
    private ASId embeddedId = new ASId();

    @ManyToOne
    @MapsId("answerId")
    @JoinColumn(name = "answerId")
    @Setter
    private BAnswer answer;

    @Basic
    @Setter
    private boolean clean;

    @Embeddable
    @Getter
    @Setter
    public static class ASId implements Serializable {
        @Basic
        private Integer answerId;
        @Basic
        private Integer yarnId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ASId id = (ASId) o;
            return Objects.equals(answerId, id.answerId) &&
                    Objects.equals(yarnId, id.yarnId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(answerId, yarnId);
        }
    }
}
