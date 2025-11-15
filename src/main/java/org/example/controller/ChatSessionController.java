package org.example.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.dto.DialogRecordDto;
import org.example.entity.ChatSession;
import org.example.repository.ChatSessionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/sessions")
@CrossOrigin(origins = "http://localhost:3000")
public class ChatSessionController {

    private final ChatSessionRepository chatSessionRepository;
    private final ObjectMapper objectMapper;

    // 기록 페이지용: 유저별 최신순
    @GetMapping
    public List<ChatSession> list(@RequestParam Long userId) {
        return chatSessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // 힌트 사용 1회 증가
    @PostMapping("/{id}/hint")
    public void incHint(@PathVariable UUID id) {
        chatSessionRepository.findById(id).ifPresent(s -> {
            s.setHintsUsed((s.getHintsUsed() == null ? 0 : s.getHintsUsed()) + 1);
            chatSessionRepository.save(s);
        });
    }

    // 정답 성공 처리
    @PostMapping("/{id}/solve")
    public void markSolved(@PathVariable UUID id) {
        chatSessionRepository.findById(id).ifPresent(s -> {
            s.setSolved(true);
            chatSessionRepository.save(s);
        });
    }

    // 대화 메시지 조회 (RecordPage → onReplay 에서 사용)
    @GetMapping("/{id}/messages")
    public ResponseEntity getMessages(@PathVariable UUID id) {
        return chatSessionRepository.findById(id)
                .map(s -> {
                    String raw = s.getMessages();
                    if (raw == null || raw.isBlank()) {
                        // 저장된 메시지가 없으면 빈 배열
                        return ResponseEntity.<List<DialogRecordDto>>ok(List.of());
                    }
                    try {
                        List<DialogRecordDto> list = objectMapper.readValue(
                                raw,
                                new TypeReference<List<DialogRecordDto>>() {}
                        );
                        return ResponseEntity.<List<DialogRecordDto>>ok(list);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return ResponseEntity.<List<DialogRecordDto>>internalServerError().build();
                    }
                })
                .orElseGet(() -> ResponseEntity.<List<DialogRecordDto>>notFound().build());
    }
}
