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
public class ProblemController {

    private final ProblemFetcherService fetcher;
    private final ProblemRepository problemRepository;
    private final CurrentProblemStore currentProblemStore;
    private final ChatSessionRepository chatSessionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 보기 좋은 구분선(유니코드/ASCII)
    private static final String SEP =
            "\n────────────────────────────────────────\n";

    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping("/random-by-level")
    public ResponseEntity<String> randomByLevel(
            @RequestParam int level,
            @RequestParam(required = false) String username
    ) {
        Problem p = fetcher.fetchFromDbByLevel(level);
        if (p == null) return ResponseEntity.noContent().build();

        // 1) 현재 사용자별 "배정 문제 UUID" 유지(힌트·채점에서 사용)
        if (username != null && !username.isBlank()) {
            currentProblemStore.put(username, p.getId());
        }

        // 2) 대화(세션) 저장 — 핵심은 problemId(UUID)만 넣는 것!
        UUID sessionId = null;
        if (username != null && !username.isBlank()) {
            ChatSession s = ChatSession.builder()
                    .problemId(p.getId())
                    .difficulty(p.getDifficulty())
                    .username(username)
                    .title(Optional.ofNullable(p.getTitle()).orElse("제목 없음"))
                    .hintsUsed(0)
                    .solved(false)
                    .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                    .build();
            chatSessionRepository.save(s);
            sessionId = s.getId();
        }

        // 3) 프론트로 내려줄 표시 문자열
        String body = buildProblemText(p);
        var rb = ResponseEntity.ok();
        if (sessionId != null) rb.header("X-Session-Id", sessionId.toString());
        return rb.body(body);
    }

    // 세션 ID로 다시 불러오기 (RecordPage “다시보기”에서 사용)
    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping("/by-session")
    public ResponseEntity<String> loadBySession(@RequestParam UUID sessionId) {
        var s = chatSessionRepository.findById(sessionId).orElse(null);
        if (s == null) return ResponseEntity.notFound().build();

        var p = problemRepository.findById(s.getProblemId()).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();

        // (선택) currentProblemStore를 쓰고 있다면 유지
        if (s.getUsername() != null && !s.getUsername().isBlank()) {
            currentProblemStore.put(s.getUsername(), p.getId());
        }

        String body = buildProblemText(p);
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
