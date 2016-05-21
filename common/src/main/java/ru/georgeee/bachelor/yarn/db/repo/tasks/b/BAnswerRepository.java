package ru.georgeee.bachelor.yarn.db.repo.tasks.b;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.georgeee.bachelor.yarn.db.entity.tasks.b.BAnswer;

public interface BAnswerRepository extends JpaRepository<BAnswer, Integer> {
}
