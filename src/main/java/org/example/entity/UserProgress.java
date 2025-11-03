package org.example.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long problemId;

    private String status; // STARTED, SOLVED, FAILED
    private Integer attempts = 0;
    private Long solveTimeSeconds; // 풀이 시간
    private LocalDateTime createdAt = LocalDateTime.now();
}