package org.example.service;

import org.example.entity.AppUser;
import org.example.entity.Problem;
import org.example.entity.UserAnswer;
import org.example.repository.UserAnswerRepository;
import org.springframework.stereotype.Service;



@Service
public class UserAnswerService {

    private final UserAnswerRepository userAnswerRepository;
    private final OpenAiClient openAiClient;

    public UserAnswerService(UserAnswerRepository userAnswerRepository, OpenAiClient openAiClient) {
        this.userAnswerRepository = userAnswerRepository;
        this.openAiClient = openAiClient;
    }

    /**
     * 사용자의 답변을 ChatGPT에 보내서 정답 여부를 판별하는 메서드
     */
    public String evaluateUserAnswer(AppUser user, Problem problem, String userCode, String language) {
        try {
            // ✅ 코드 형태인지 간단히 확인
            if (!isCodeFormat(userCode)) {
                return "⚠️ 코드 형태로 답변을 입력해주세요.";
            }

            // ✅ ChatGPT에 보낼 system / user 프롬프트 구성
            String systemPrompt = """
                당신은 숙련된 알고리즘 채점관입니다.
                사용자가 제출한 코드를 보고 정답 여부를 판단하세요.
                - 언어는 C, Java, Python 등이 될 수 있습니다.
                - 반드시 '정답입니다.' 또는 '틀렸습니다.' 중 하나로 시작하세요.
                - 이유가 있다면 간단히 한 줄로 설명하세요.
            """;

            // ✅ questionId 제거 → problem.getId() 사용
            String userPrompt = String.format("""
                문제 ID: %d
                언어: %s
                코드:
                ```
                %s
                ```
            """, problem.getId(), language, userCode);

            // ✅ ChatGPT로 평가 요청
            String result = openAiClient.chat(systemPrompt, userPrompt);

            // ✅ 결과가 "정답입니다"로 시작하는지 판별
            boolean isCorrect = result.trim().startsWith("정답");

            // ✅ DB 저장
            UserAnswer answer = new UserAnswer();
            answer.setUser(user);
            answer.setProblem(problem);
            answer.setAnswer(userCode);
            answer.setCorrect(isCorrect);

            userAnswerRepository.save(answer);

            // ✅ 결과 반환
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return "⚠️ 답변 평가 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    /**
     * 코드 형식인지 간단히 확인 (예: 세미콜론, 중괄호, def/class 등 포함 여부)
     */
    private boolean isCodeFormat(String text) {
        String lower = text.toLowerCase();
        return lower.contains(";") || lower.contains("{") || lower.contains("class")
                || lower.contains("def ") || lower.contains("public static void main");
    }
}