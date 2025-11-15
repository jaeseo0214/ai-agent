// src/main/java/org/example/entity/ChatSession.java
package org.example.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "chat_session", schema = "core")
public class ChatSession {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)                 // Postgres uuid 매핑
    @Column(name = "session_id", nullable = false, columnDefinition = "uuid")
    private UUID sessionId;

    // ✅ Problem 연관관계 대신, problem_id(UUID)만 저장
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "problem_id", nullable = false, columnDefinition = "uuid")
    private UUID problemId;

    // 참조 사용자 ID
    @Column(name = "user_id")
    private Long userId;

    // 난이도(정수)
    @Column(name = "difficulty", nullable = false)
    private Integer difficulty;

    // 세션 생성 시각 (UTC 권장)
    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    // 문제 제목
    @Column(name = "title", length = 255, nullable = false)
    private String title;

    // 사용한 힌트 수 (기본 0)
    @Column(name = "hints_used")
    private Integer hintsUsed;

    // 정답 여부
    @Column(name = "solved", nullable = false)
    private Boolean solved;

    // 대화 기록 메세지
    @Builder.Default
    @Column(name = "messages", columnDefinition = "text", nullable = false)
    private String messages = "[]";

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
        if (hintsUsed == null) hintsUsed = 0;
        if (solved == null) solved = false;
    }
}
