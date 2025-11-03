package org.example.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Problem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 10000)
    private String description;   // HTML 허용 가능

    // 난이도: 1~5 (정수형으로 관리하면 범위 제어 쉬움)
    private Integer level;

    private String topic;         // "DP", "BFS" 등 태그
    private String sourceUrl;     // 출처 URL

    @Column(length = 2000)
    private String note;          // 내부 메모 (예: 크롤링 시 주의점)

    @Column(length = 3000)
    private String exampleInput;   // 예시 입력

    @Column(length = 3000)
    private String exampleOutput;  // 예시 출력
}