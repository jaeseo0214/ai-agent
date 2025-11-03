// ChatSessionController.java
package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.entity.ChatSession;
import org.example.repository.ChatSessionRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/sessions")
@CrossOrigin(origins = "http://localhost:3000")
public class ChatSessionController {

    private final ChatSessionRepository repo;

    // 기록 페이지용: 유저별 최신순
    @GetMapping
    public List<ChatSession> list(@RequestParam String username) {
        return repo.findByUsernameOrderByCreatedAtDesc(username);
    }

    // 힌트 사용 1회 증가
    @PostMapping("/{id}/hint")
    public void incHint(@PathVariable UUID id) {
        repo.findById(id).ifPresent(s -> {
            s.setHintsUsed((s.getHintsUsed() == null ? 0 : s.getHintsUsed()) + 1);
            repo.save(s);
        });
    }

    // 정답 성공 처리
    @PostMapping("/{id}/solved")
    public void markSolved(@PathVariable UUID id) {
        repo.findById(id).ifPresent(s -> {
            s.setSolved(true);
            repo.save(s);
        });
    }
}
