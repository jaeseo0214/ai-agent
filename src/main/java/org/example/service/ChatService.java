package org.example.service;

import jakarta.transaction.Transactional;
import org.example.entity.Problem;
import org.example.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Service
public class ChatService {

    private final ProblemFetcherService fetcherService;
    private final ProblemRepository problemRepository;
    private final HintService hintService;
    private final OpenAiClient openAiClient;

    private static final Pattern hintPattern = Pattern.compile("힌트\\s*(\\d+)");

    @Transactional // 11/2추가
    public String handleMessage(String username, String message) {
        try {
            Matcher hm = hintPattern.matcher(message);
            if (hm.find()) {
                int step = Integer.parseInt(hm.group(1));

                // ★ username → problemId → DB
                UUID pid = CurrentProblemStore.get(username);
                if (pid == null) {
                    return "현재 배정된 문제가 없습니다. 먼저 난이도를 선택해 문제를 받아주세요.";
                }
                Problem cp = problemRepository.findById(pid).orElse(null);
                if (cp == null) {
                    return "문제를 찾지 못했습니다. 다시 문제를 받아주세요.";
                }

                String html = cp.getBody() == null ? "" : cp.getBody();
                if (html.isBlank()) {
                    return "이 문제의 본문이 비어 있습니다. 다른 문제를 받아주세요.";
                }
                return hintService.generateHint(html, step);
            }

            // 3) 자바로 풀어줘 요청?
            if (message.contains("자바로 풀어줘")) {
                Problem last = problemRepository.findAll()
                        .stream()
                        .reduce((first, second) -> second)
                        .orElse(null);
                if (last == null) return "먼저 문제를 가져와주세요.";
                String prompt = "다음 문제를 Java로 풀어주는 코드를 제공해줘. 단, 주석으로 풀이 설명을 추가해줘.\n문제:\n" +
                        last.getDescription().replaceAll("<[^>]*>", "");
                return openAiClient.chat("You are an expert Java programmer.", prompt);
            }

            // 4) 일반 대화는 AI에 위임
            return openAiClient.chat("You are a helpful coding assistant.", message);

        } catch (Exception e) {
            e.printStackTrace();
            return "처리 중 오류가 발생했습니다: " + e.getMessage();
        }
    }
}