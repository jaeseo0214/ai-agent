package org.example.dto;

import lombok.Data;

// 대화 기록을 위한 DTO
@Data
public class DialogRecordDto {
    private double at;       // 타임스탬프(초 단위)
    private String code;     // 제출 코드 (없으면 null)
    private String role;     // "user" / "assistant"
    private String content;  // "코드 제출", "힌트 1", "정답입니다..." 등
    private String language; // "C", "Java" 등 (없으면 null)
}
