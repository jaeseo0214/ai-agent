// ProblemController.java
package org.example.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.entity.Problem;
import org.example.service.CurrentProblemStore;
import org.example.service.ProblemFetcherService;
import org.example.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemFetcherService fetcher;
    private final ProblemRepository problemRepository;
    private final CurrentProblemStore currentProblemStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 유니코드 구분선 (박스 드로잉 문자)
    private static final String SEP = "\n──────────────────────────────────────────\n";

    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping("/random-by-level")
    public ResponseEntity<String> randomByLevel(
            @RequestParam int level,
            @RequestParam(required = false) String username
    ) {
        Problem p = fetcher.fetchFromDbByLevel(level);
        if (p == null) {
            return ResponseEntity.noContent().build();
        }

        // 사용자별 현재 문제 UUID 저장 (힌트/채점에서 사용)
        if (username != null && !username.isBlank()) {
            currentProblemStore.put(username, p.getId());
        }

        // 본문(HTML → 텍스트)
        String bodyHtml = Optional.ofNullable(p.getDescription()).orElse("");
        String bodyText = cleanHtml(bodyHtml);

        // 타이틀 라인: [난이도명] 제목
        String titleLine = String.format("[%s] %s",
                Optional.ofNullable(p.getDifficultyTitle()).orElse(""),
                Optional.ofNullable(p.getTitle()).orElse("제목 없음"));

        // 입력/출력/예제 섹션
        String ioSections = buildIoSections(p);

        // 최종 문자열 (기존 포맷 유지 + 구분선 추가)
        StringBuilder sb = new StringBuilder();
        sb.append(titleLine).append(SEP)
                .append("문제:\n").append(bodyText).append(SEP)
                .append(ioSections);

        return ResponseEntity.ok(sb.toString());
    }

    // ====== 헬퍼 ======

    // HTML 태그 제거 + 개행 보정
    private String cleanHtml(String html) {
        String t = html.replaceAll("(?is)<br\\s*/?>", "\n")
                .replaceAll("(?is)</p>", "\n")
                .replaceAll("(?is)<[^>]+>", "")
                .replace("&nbsp;", " ")
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .trim();
        return t;
    }

    // 입력/출력/예제 렌더링 (유니코드 구분선 사용)
    private String buildIoSections(Problem p) {
        StringBuilder sb = new StringBuilder();

        String in = Optional.ofNullable(p.getInputDesc()).map(String::trim).orElse("");
        String out = Optional.ofNullable(p.getOutputDesc()).map(String::trim).orElse("");
        String samplesJson = Optional.ofNullable(p.getSamples()).orElse("");

        boolean wroteSomething = false;

        if (!in.isBlank()) {
            sb.append("입력:\n").append(in).append(SEP);
            wroteSomething = true;
        }
        if (!out.isBlank()) {
            sb.append("출력:\n").append(out).append(SEP);
            wroteSomething = true;
        }

        if (!samplesJson.isBlank()) {
            try {
                JsonNode arr = objectMapper.readTree(samplesJson);
                if (arr.isArray()) {
                    int idx = 1;
                    for (JsonNode node : arr) {
                        String name   = node.path("name").asText("");
                        String input  = node.path("input").asText("");
                        String output = node.path("output").asText("");

                        String inTitle  = name.isBlank() ? ("예제 입력 "  + idx) : name.replace("출력", "입력");
                        String outTitle = name.isBlank() ? ("예제 출력 " + idx) : name.replace("입력", "출력");

                        if (!input.isEmpty()) {
                            sb.append(inTitle).append(":\n")
                                    .append(input).append("\n");
                        }
                        if (!output.isEmpty()) {
                            sb.append(outTitle).append(":\n")
                                    .append(output).append("\n");
                        }

                        // 각 예제 블록 끝에 구분선 추가
                        sb.append(SEP);
                        wroteSomething = true;
                        idx++;
                    }
                }
            } catch (Exception ignore) {
                // samples 파싱 실패 시 본문만 보여줘도 되므로 무시
            }
        }

        // 마지막이 SEP로 끝나면 깔끔하게 제거
        if (wroteSomething && sb.length() >= SEP.length()) {
            int from = sb.length() - SEP.length();
            if (sb.substring(from).equals(SEP)) {
                sb.delete(from, sb.length());
            }
        }

        return sb.toString();
    }
}
