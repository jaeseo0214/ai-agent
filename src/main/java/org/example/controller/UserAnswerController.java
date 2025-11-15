package org.example.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.entity.LogInEntity;
import org.example.entity.Problem;
import org.example.repository.AppUserRepository;
import org.example.repository.ChatSessionRepository;
import org.example.repository.ProblemRepository;
import org.example.service.CurrentProblemStore;
import org.example.service.UserAnswerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/answers")
@RequiredArgsConstructor
public class UserAnswerController {

    private final AppUserRepository appUserRepository;
    private final ProblemRepository problemRepository;
    private final UserAnswerService userAnswerService;
    private final ChatSessionRepository chatSessionRepository;
    private final ObjectMapper objectMapper;

    /**
     * 사용자가 제출한 코드/답안을 평가합니다.
     * - JSON 바디와 쿼리스트링(폼) 파라미터 모두 허용
     * - problemId가 없으면 CurrentProblemStore에서 username 기준으로 최근 배정 문제 사용
     * - 카드용 세션(가장 최근 1건) 갱신: 시도수 +1, 정답이면 solved=true
     */
    @PostMapping("/evaluate")
    public ResponseEntity<String> evaluate(
            @RequestParam(value = "userId", required = false) Long userIdParam,
            @RequestParam(value = "code", required = false) String codeParam,
            @RequestParam(value = "language", required = false) String languageParam,
            @RequestParam(value = "problemId", required = false) String problemIdParam,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        // 1) 파라미터/바디 병합
        Long userId       = firstNonNull(userIdParam,tryParseLong(asString(get(body, "userId"))));
        String code       = firstNonBlank(codeParam, asString(get(body, "code")), asString(get(body, "answer")));
        String language   = firstNonBlank(languageParam, asString(get(body, "language")));
        String problemStr = firstNonBlank(problemIdParam, asString(get(body, "problemId")));

        if (userId == null) {
            return ResponseEntity.badRequest().body("userId is required");
        }
        if (isBlank(code) || isBlank(language)) {
            return ResponseEntity.badRequest().body("code and language are required");
        }

        // 2) 사용자 로드
        LogInEntity user = appUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // 3) 문제 식별
        UUID pid = tryParseUuid(problemStr);
        if (pid == null) {
            pid = CurrentProblemStore.get(userId);  // 인스턴스 메서드 사용
        }
        if (pid == null) {
            return ResponseEntity.badRequest().body("현재 배정된 문제가 없습니다. 먼저 문제를 받으세요.");
        }

        final UUID lookupPid = pid;

        Problem problem = problemRepository.findById(lookupPid)
                .orElseThrow(() -> new RuntimeException("문제를 찾을 수 없습니다: " + lookupPid));

        // 4) 평가
        String resultMessage = userAnswerService.evaluateUserAnswer(user, problem, code, language);
        boolean isCorrect = resultMessage != null && resultMessage.replace(" ", "").contains("정답");

        // 5) 사용자 레벨 갱신
        int currentLevel = Optional.ofNullable(user.getLevel()).orElse(1); // 현재(다음에 도전할) 레벨
        int problemLevel = Optional.ofNullable(problem.getLevel()).orElse(currentLevel); // 문제 레벨

        int maxLevel = 8;
        int newLevel;
        if (isCorrect) { // 맞추면 "푼 문제의 레벨 + 1" 까지 올라감
            int candidate = problemLevel + 1;
            newLevel = Math.min(maxLevel, Math.max(currentLevel, candidate));
        } else { // 틀리면 레벨은 유지 (다운 X)
            newLevel = currentLevel;
        }

        user.setLevel(newLevel);
        appUserRepository.save(user);

        // 6) 카드용 세션 갱신(가장 최근 세션 1건)
        chatSessionRepository
                .findTopByUserIdAndProblemIdOrderByCreatedAtDesc(userId, pid)
                .ifPresent(s -> {
                    // ---- 시도/정답 ----
                    int tries = (s.getHintsUsed() == null) ? 0 : s.getHintsUsed();
                    s.setHintsUsed(tries + 1);
                    if (isCorrect) s.setSolved(Boolean.TRUE);

                    // ---- messages 갱신 ----
                    String current = s.getMessages();  // TEXT 컬럼
                    List<Map<String, Object>> list;

                    try {
                        if (current == null || current.isBlank() || "[]".equals(current.trim())) {
                            list = new ArrayList<>();
                        } else {
                            list = objectMapper.readValue(
                                    current,
                                    new TypeReference<List<Map<String, Object>>>() {}
                            );
                        }
                    } catch (Exception e) {
                        // 파싱 실패하면 새로 시작
                        list = new ArrayList<>();
                    }

                    double now = System.currentTimeMillis() / 1000.0;

                    Map<String, Object> userMsg = new LinkedHashMap<>();
                    userMsg.put("at", now);
                    userMsg.put("code", code);
                    userMsg.put("role", "user");
                    userMsg.put("content", "코드 제출");
                    userMsg.put("language", language);

                    Map<String, Object> assistantMsg = new LinkedHashMap<>();
                    assistantMsg.put("at", now);
                    assistantMsg.put("code", null);
                    assistantMsg.put("role", "assistant");
                    assistantMsg.put("content", resultMessage);
                    assistantMsg.put("language", null);

                    list.add(userMsg);
                    list.add(assistantMsg);

                    try {
                        String updated = objectMapper.writeValueAsString(list);
                        s.setMessages(updated);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }

                    chatSessionRepository.save(s);
                });

        return ResponseEntity.ok(resultMessage);
    }

    // ------------------ helpers ------------------

    private static Object get(Map<String, Object> map, String key) {
        return map == null ? null : map.get(key);
    }

    private static String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static String firstNonBlank(String... vals) {
        if (vals == null) return null;
        for (String v : vals) if (!isBlank(v)) return v;
        return null;
    }

    private static <T> T firstNonNull(T... vals) {
        if (vals == null) return null;
        for (T v : vals) if (v != null) return v;
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static UUID tryParseUuid(String s) {
        if (isBlank(s)) return null;
        try { return UUID.fromString(s.trim()); }
        catch (Exception ignored) { return null; }
    }

    private static Long tryParseLong(String s) {
        if (isBlank(s)) return null;
        try { return Long.parseLong(s.trim()); }
        catch (Exception ignored) { return null; }
    }
}
