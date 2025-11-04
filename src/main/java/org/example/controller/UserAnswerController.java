package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.entity.AppUser;
import org.example.entity.Problem;
import org.example.repository.AppUserRepository;
import org.example.repository.ChatSessionRepository;
import org.example.repository.ProblemRepository;
import org.example.service.CurrentProblemStore;
import org.example.service.UserAnswerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/answers")
@RequiredArgsConstructor
public class UserAnswerController {

    private final AppUserRepository appUserRepository;
    private final ProblemRepository problemRepository;
    private final UserAnswerService userAnswerService;
    private final ChatSessionRepository chatSessionRepository;
    private final CurrentProblemStore currentProblemStore;

    /**
     * 사용자가 제출한 코드/답안을 평가합니다.
     * - JSON 바디와 쿼리스트링(폼) 파라미터 모두 허용
     * - problemId가 없으면 CurrentProblemStore에서 username 기준으로 최근 배정 문제 사용
     * - 카드용 세션(가장 최근 1건) 갱신: 시도수 +1, 정답이면 solved=true
     */
    @PostMapping("/evaluate")
    public ResponseEntity<String> evaluate(
            @RequestParam(value = "username", required = false) String usernameParam,
            @RequestParam(value = "code", required = false) String codeParam,
            @RequestParam(value = "language", required = false) String languageParam,
            @RequestParam(value = "problemId", required = false) String problemIdParam,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        // 1) 파라미터/바디 병합
        String username   = firstNonBlank(usernameParam, asString(get(body, "username")));
        String code       = firstNonBlank(codeParam, asString(get(body, "code")), asString(get(body, "answer")));
        String language   = firstNonBlank(languageParam, asString(get(body, "language")));
        String problemStr = firstNonBlank(problemIdParam, asString(get(body, "problemId")));

        if (isBlank(username)) {
            return ResponseEntity.badRequest().body("username is required");
        }
        if (isBlank(code) || isBlank(language)) {
            return ResponseEntity.badRequest().body("code and language are required");
        }

        // 2) 사용자 로드
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // 3) 문제 식별
        UUID pid = tryParseUuid(problemStr);
        if (pid == null) {
            pid = currentProblemStore.get(username);  // 인스턴스 메서드 사용
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

        // 5) 카드용 세션 갱신(가장 최근 세션 1건)
        chatSessionRepository
                .findTopByUsernameAndProblemIdOrderByCreatedAtDesc(username, pid)
                .ifPresent(s -> {
                    int tries = (s.getHintsUsed() == null) ? 0 : s.getHintsUsed();
                    s.setHintsUsed(tries + 1);
                    if (isCorrect) s.setSolved(Boolean.TRUE);
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

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static UUID tryParseUuid(String s) {
        if (isBlank(s)) return null;
        try { return UUID.fromString(s.trim()); }
        catch (Exception ignored) { return null; }
    }
}
