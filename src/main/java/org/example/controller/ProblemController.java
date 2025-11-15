// src/main/java/org/example/controller/ProblemController.java
package org.example.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.entity.ChatSession;
import org.example.entity.Problem;
import org.example.repository.ChatSessionRepository;
import org.example.repository.ProblemRepository;
import org.example.service.CurrentProblemStore;
import org.example.service.ProblemFetcherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/problems")
@CrossOrigin(origins = "http://localhost:3000")
public class ProblemController {

    private final ProblemFetcherService fetcher;
    private final ProblemRepository problemRepository;
    private final CurrentProblemStore currentProblemStore;
    private final ChatSessionRepository chatSessionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 구분선
    private static final String SEP =
            "\n────────────────────────────────────────\n";
    /**
     * 레벨 기반 랜덤 문제 출제
     * - DB에서 level로 문제 뽑기
     * - userId가 있으면 chat_session 레코드 생성 + currentProblemStore 에 문제 ID 저장
     * - 응답 헤더 X-Session-Id 로 세션 ID 전달
     */
    @GetMapping("/random-by-level")
    public ResponseEntity<String> randomByLevel(
            @RequestParam int level,
            @RequestParam(required = false) Long userId
    ) {
        Problem p = fetcher.fetchFromDbByLevel(level);
        if (p == null) return ResponseEntity.noContent().build();

        // 1) 사용자별 현재 배정 문제 기억
        if (userId != null) {
            currentProblemStore.put(userId, p.getId());
        }

        // 2) chat_session 생성 (problemId만 연결)
        UUID sessionId = null;
        if (userId != null) {
            ChatSession s = ChatSession.builder()
                    .problemId(p.getId())
                    .difficulty(p.getDifficulty())
                    .userId(userId)
                    .title(Optional.ofNullable(p.getTitle()).orElse("제목 없음"))
                    .hintsUsed(0)
                    .solved(false)
                    .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                    .messages("[]")
                    .build();
            chatSessionRepository.save(s);
            sessionId = s.getSessionId();
        }

        // 3) 프론트에 내려줄 표시 문자열
        String body = buildProblemText(p);

        var rb = ResponseEntity.ok();
        if (sessionId != null) rb.header("X-Session-Id", sessionId.toString());
        return rb.body(body);
    }

    /**
     * 세션 ID로 문제 텍스트 복구 (RecordPage "다시보기"에서 사용)
     * chat_session.problem_id 로 문제 찾기
     */
    // 세션 ID로 다시 불러오기 (RecordPage “다시보기”에서 사용)
    @GetMapping("/by-session")
    public ResponseEntity<String> loadBySession(@RequestParam("sessionId") UUID sessionId) {
        Optional<ChatSession> optSession = chatSessionRepository.findById(sessionId);
        if (optSession.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ChatSession s = optSession.get();

        // problem_id 로 조회
        Problem problem = null;
        UUID pid = s.getProblemId();
        if (pid != null) {
            problem = problemRepository.findById(pid).orElse(null);
        }

        if (s.getUserId() != null) {
            currentProblemStore.put(s.getUserId(), problem.getId());
        }

        String body = buildProblemText(problem);
        return ResponseEntity.ok(body);
    }

    /* ====================== 표시 문자열 조립부 ====================== */

    private String buildProblemText(Problem p) {
        // 본문(HTML → 텍스트)
        String bodyHtml = Optional.ofNullable(p.getDescription()).orElse("");
        String bodyText = cleanHtml(bodyHtml);

        // 타이틀 라인: [난이도명] 제목
        String titleLine = String.format("[%s] %s",
                Optional.ofNullable(p.getDifficultyTitle()).orElse(""),
                Optional.ofNullable(p.getTitle()).orElse("제목 없음"));

        // 입력/출력/예제
        String ioSections = buildIoSections(p);

        // 최종 문자열 (구분선 포함)
        StringBuilder sb = new StringBuilder();
        sb.append(titleLine).append(SEP)
                .append("문제:\n").append(bodyText).append(SEP)
                .append(ioSections);

        return sb.toString();
    }

    // HTML 태그 제거 + 공백 정리
    private String cleanHtml(String html) {
        String t = html.replaceAll("(?is)<br\\s*/?>", "\n");
        t = t.replaceAll("(?is)</p>", "\n");
        t = t.replaceAll("(?is)<[^>]+>", "");
        t = t.replace("&nbsp;", " ");
        t = t.replaceAll("[ \\t\\x0B\\f\\r]+", " ").trim();
        return t;
    }

    // 입력/출력/예제 렌더링 (DB의 input_desc, output_desc, samples 사용)
    private String buildIoSections(Problem p) {
        StringBuilder sb = new StringBuilder();

        String in = Optional.ofNullable(p.getInputDesc()).map(String::trim).orElse("");
        String out = Optional.ofNullable(p.getOutputDesc()).map(String::trim).orElse("");
        String samplesJson = Optional.ofNullable(p.getSamples()).orElse("");

        if (!in.isBlank()) {
            sb.append("입력:\n").append(in).append(SEP);
        }
        if (!out.isBlank()) {
            sb.append("출력:\n").append(out).append(SEP);
        }

        if (!samplesJson.isBlank()) {
            try {
                JsonNode arr = objectMapper.readTree(samplesJson);
                if (arr.isArray()) {
                    int idx = 1;
                    for (JsonNode node : arr) {
                        String name   = node.path("name").asText("");  // 옵션
                        String input  = node.path("input").asText("");
                        String output = node.path("output").asText("");

                        String inTitle  = name.isBlank() ? ("예제 입력 " + idx)  : name.replace("출력", "입력");
                        String outTitle = name.isBlank() ? ("예제 출력 " + idx)  : name.replace("입력", "출력");

                        if (!input.isEmpty()) {
                            sb.append(inTitle).append(":\n").append(input).append(SEP);
                        }
                        if (!output.isEmpty()) {
                            sb.append(outTitle).append(":\n").append(output).append(SEP);
                        }
                        idx++;
                    }
                }
            } catch (Exception ignore) {
                // samples 파싱 실패는 무시 (본문만 표기)
            }
        }
        return sb.toString();
    }
}
