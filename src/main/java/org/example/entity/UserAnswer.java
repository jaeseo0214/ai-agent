package org.example.entity;

import jakarta.persistence.Entity;
import lombok.*;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class UserAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 답변 고유 번호

    @ManyToOne
    private AppUser user;      // 누가 풀었는지

    @ManyToOne
    private Problem problem; // 어떤 문제를 풀었는지

    private String answer;  // 사용자가 입력한 답
    private Boolean correct; // 정답 여부
}
