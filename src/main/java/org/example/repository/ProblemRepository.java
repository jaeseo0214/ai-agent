package org.example.repository;

import org.example.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProblemRepository extends JpaRepository<Problem, Long> {
    List<Problem> findByLevel(Integer level);
    List<Problem> findByTopic(String topic);
    List<Problem> findByLevelBetween(int low, int high);
}