package org.example.repository;

import org.example.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    // 채점용: userId + problemId 기준, 가장 최근 1건
    Optional<ChatSession> findTopByUserIdAndProblemIdOrderByCreatedAtDesc(
            Long userId, UUID problemId);

    // 기록용: 한 유저의 전체 세션 목록
    List<ChatSession> findByUserIdOrderByCreatedAtDesc(Long userId);
}
