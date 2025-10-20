package org.example.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class HintService {

    private final OpenAiClient openAiClient;

    public String generateHint(String problemText, int step) {
        String system = "너는 친절한 코딩 튜터야. 절대 정답 코드를 바로 제공하지 마.";
        String userPrompt = switch (step) {
            case 1 -> "1단계(문제 이해): 문제의 목적과 입력/출력 형식을 간단히 설명해줘.\n문제:\n" + problemText;
            case 2 -> "2단계(접근): 어떤 자료구조/알고리즘으로 접근할지 설명하고 핵심 아이디어(시간복잡도 포함)를 알려줘.\n문제:\n" + problemText;
            case 3 -> "3단계(구조): 의사코드 수준의 코드 구조(함수 이름, 핵심 반복/기저조건)만 제시해줘.\n문제:\n" + problemText;
            default -> "추가 힌트: 좀 더 구체적인 도움이 필요하면 어떤 부분이 어려운지 말해줘.\n문제:\n" + problemText;
        };

        try {
            return openAiClient.chat(system, userPrompt);
        } catch (Exception e) {
            return "힌트 생성 실패: " + e.getMessage();
        }
    }
}