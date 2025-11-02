package org.example.repository;

import org.example.entity.UserAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAnswerRepository extends JpaRepository<UserAnswer, Long> {
}