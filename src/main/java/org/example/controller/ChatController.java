package org.example.controller;

import org.example.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody Map<String, Object> body) {
        // userId는 Long으로 파싱
        Object rawUserId = body.get("userId");
        Long userId = null;
        if (rawUserId != null) {
            try {
                userId = Long.valueOf(String.valueOf(rawUserId));
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "userId must be a number"));
            }
        }
        String message = (String) body.get("message");
        String reply = chatService.handleMessage(userId, message);
        return ResponseEntity.ok(Map.of("reply", reply));
    }
}