package ru.georgeee.bachelor.yarn.db.repo.tasks.a;


import org.springframework.data.jpa.repository.JpaRepository;
import ru.georgeee.bachelor.yarn.db.entity.tasks.a.Pool;

public interface PoolRepository extends JpaRepository<Pool, Integer> {
}

