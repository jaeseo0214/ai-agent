package org.example.repository;

import org.example.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    // 유저 + 문제 기준으로 "가장 최근 1건"
    Optional<ChatSession> findTopByUsernameAndProblemIdOrderByCreatedAtDesc(
            String username, UUID problemId);

    // 기록 페이지(최신순 목록) 용
    List<ChatSession> findByUsernameOrderByCreatedAtDesc(String username);
}
