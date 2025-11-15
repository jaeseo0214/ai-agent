package org.example.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.example.entity.Problem;
import org.example.repository.ChatSessionRepository;
import org.example.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.example.dto.DialogRecordDto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Service
public class ChatService {

    private final ProblemRepository problemRepository;
    private final HintService hintService;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;
    private final ChatSessionRepository chatSessionRepository;

    private static final Pattern hintPattern = Pattern.compile("힌트\\s*(\\d+)");

    @Transactional // 11/2추가
    public String handleMessage(Long userId, String message) {
        try {
            Matcher hm = hintPattern.matcher(message);
            if (hm.find()) {
                int step = Integer.parseInt(hm.group(1));

                if (userId == null) {
                    return "현재 배정된 문제가 없습니다. 먼저 난이도를 선택해 문제를 받아주세요.";
                }

                // userId → 현재 배정된 problemId
                UUID pid = CurrentProblemStore.get(userId);
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

                // 1) 힌트 생성
                String hintText = hintService.generateHint(html, step);

                // 2) 대화 기록에 [힌트 요청 / 힌트 응답] 추가
                appendHintDialog(userId, cp.getId(), message, hintText);

                // 3) 프론트로는 힌트 텍스트만 반환
                return hintText;
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
    private void appendHintDialog(Long userId,
                                  UUID problemId,
                                  String userContent,
                                  String assistantContent) {

        chatSessionRepository
                .findTopByUserIdAndProblemIdOrderByCreatedAtDesc(userId, problemId)
                .ifPresent(s -> {
                    try {
                        String raw = s.getMessages();

                        // 기존 messages → List<DialogRecordDto>
                        List<DialogRecordDto> list;
                        if (raw == null || raw.isBlank()) {
                            list = new ArrayList<>();
                        } else {
                            list = objectMapper.readValue(
                                    raw,
                                    new TypeReference<List<DialogRecordDto>>() {}
                            );
                        }

                        double now = System.currentTimeMillis() / 1000.0;

                        // 유저가 보낸 힌트 요청 ("힌트 1" 등)
                        DialogRecordDto userMsg = new DialogRecordDto();
                        userMsg.setAt(now);
                        userMsg.setCode(null);
                        userMsg.setRole("user");
                        userMsg.setContent(userContent);  // 예: "힌트 1"
                        userMsg.setLanguage(null);

                        // AI가 돌려준 힌트 내용
                        DialogRecordDto botMsg = new DialogRecordDto();
                        botMsg.setAt(now);
                        botMsg.setCode(null);
                        botMsg.setRole("assistant");
                        botMsg.setContent(assistantContent);
                        botMsg.setLanguage(null);

                        list.add(userMsg);
                        list.add(botMsg);

                        // 다시 JSON으로 저장
                        s.setMessages(objectMapper.writeValueAsString(list));
                        chatSessionRepository.save(s);

                    } catch (Exception e) {
                        e.printStackTrace();
                        // 기록 실패해도 힌트 기능 자체는 돌아가게 예외는 삼켜둠
                    }
                });
    }
}