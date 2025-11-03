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
    public ResponseEntity<?> chat(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "anonymous");
        String message = body.get("message");
        String reply = chatService.handleMessage(username, message);
        return ResponseEntity.ok(Map.of("reply", reply));
    }
}