package org.example.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "problems", schema = "core")
public class Problem {
    @Id
    @Column(name = "problem_id")
    private UUID id;

    @Column(name = "external_key")
    private String key;

    @Column(name  = "title")
    private String title;

    @Column(name = "body", columnDefinition = "text")
    private String description;   // HTML 허용 가능

    @Column(name = "difficulty")
    private Integer level;

    @Column(name = "difficulty_title")
    private String topic;

    @Column(name = "meta", columnDefinition = "jsonb")
    private String sourceUrl;     // 출처 URL

    @Column(name = "input_desc")
    private String inputDesc;

    @Column(name = "output_desc")
    private String outputDesc;

    // jsonb 이지만 문자열로 받아두면 무관
    @Column(name = "samples", columnDefinition = "jsonb")
    private String samples;

    public UUID getProblemId() { return id; }
    public String getExternalKey() { return key; }
    public String getTitle() { return title; }
    public String getBody() { return description; }
    public Integer getDifficulty() { return level; }
    public String getDifficultyTitle() { return topic; }
}