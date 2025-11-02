package org.example.service;

import jakarta.transaction.Transactional;
import org.example.entity.Problem;
import org.example.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Service
public class ChatService {

    private final ProblemFetcherService fetcherService;
    private final ProblemRepository problemRepository;
    private final HintService hintService;
    private final OpenAiClient openAiClient;

    private static final Pattern BAekjoonPattern = Pattern.compile("(https?://www\\.acmicpc\\.net/problem/\\d+)");
    private static final Pattern hintPattern = Pattern.compile("힌트\\s*(\\d+)");

    @Transactional // 11/2추가
    public String handleMessage(String username, String message) {
        try {
            // 1) 문제 URL 포함?
            Matcher m = BAekjoonPattern.matcher(message);
            if (m.find()) {
                String url = m.group(1);
                Problem p = fetcherService.fetchFromBaekjoon(url);
                problemRepository.save(p);
                System.out.println("저장된 문제 ID: " + p.getId());
                // 텍스트 요약: HTML 태그 제거 후 300자
                String text = p.getDescription().replaceAll("<[^>]*>", "").trim();
                String summary = text.length() > 1000 ? text.substring(0, 1000) + "..." : text;
                System.out.println("문제 가져올 때 저장된 Description: " + p.getDescription());
                return "[" + p.getTitle() +"]" + "\n문제: " + summary;
            }

            // 2) 힌트 요청?
            Matcher hm = hintPattern.matcher(message);
            if (hm.find()) {
                int step = Integer.parseInt(hm.group(1));
                // 가장 최근 가져온 문제(간단 구현): DB에서 마지막으로 추가된 문제를 사용
                Problem last = problemRepository.findAll()
                        .stream()
                        .reduce((first, second) -> second) // get last
                        .orElse(null);

                if (last == null) return "현재 저장된 문제가 없습니다. 문제 URL을 먼저 가져와 주세요.";
                String problemText = last.getDescription().replaceAll("<[^>]*>", "");
                return hintService.generateHint(problemText, step);
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