package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.UserLogInDto;
import org.example.dto.UserSignUpDto;
import org.example.entity.LogInEntity;
import org.example.service.UserLogInService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users") // API 기본 경로
@RequiredArgsConstructor
public class UserLogInController {

    private final UserLogInService userService;

    // ------------------------
    // 회원가입
    // ------------------------
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody @Valid UserSignUpDto dto) {
        userService.register(dto);
        return ResponseEntity.ok("회원가입 성공!");
    }

    // ------------------------
    // 로그인
    // ------------------------
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody @Valid UserLogInDto dto) {
        LogInEntity user = userService.login(dto);
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "loginId", user.getLoginId(),
                "name", user.getName(),
                "level", user.getLevel()
        ));



    }
}