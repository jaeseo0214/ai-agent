package org.example.controller;


import lombok.Data;
import org.example.entity.AppUser;
import org.example.entity.Problem;
import org.example.repository.ProblemRepository;
import org.example.service.UserAnswerService;
import org.springframework.web.bind.annotation.*;
import org.example.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/answer")
@RequiredArgsConstructor
public class UserAnswerController {

    private final UserAnswerService userAnswerService;
    private final AppUserRepository appUserRepository;
    private final ProblemRepository problemRepository;



    // POST 요청으로 사용자 답변 평가
    @PostMapping("/evaluate")
    public String evaluateAnswer(@RequestBody EvaluateRequest request) {
        AppUser user = appUserRepository.findByUsername(request.getUsername());
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Problem problem = problemRepository.findById(request.getProblemId())
                .orElseThrow(() -> new RuntimeException("문제 없음"));

        return userAnswerService.evaluateUserAnswer(user, problem, request.getCode(), request.getLanguage());
    }
    @Data
    public static class EvaluateRequest {
        private String username;
        private Long problemId;
        private String code;
        private String language;
        // getter, setter
    }
}