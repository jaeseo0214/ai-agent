// package 경로는 프로젝트에 맞게
package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.repository.AppUserRepository;
import org.example.repository.ProblemRepository;
import org.example.service.UserAnswerService;
import org.example.service.CurrentProblemStore;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/answers")
@RequiredArgsConstructor
public class UserAnswerController {

    private final AppUserRepository appUserRepository;
    private final ProblemRepository problemRepository;
    private final UserAnswerService userAnswerService;
    private final CurrentProblemStore currentProblemStore;

    @PostMapping("/evaluate")
    public String evaluateAnswer(
            @RequestParam String username,
            @RequestParam(required = false) UUID problemId,  // UUID 파라미터(선택)
            @RequestParam String code,
            @RequestParam String language
    ) {
        var user = appUserRepository.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // 문제 UUID 확보: 파라미터 > 현재 배정(메모리)
        UUID pid = (problemId != null) ? problemId : currentProblemStore.get(username);
        if (pid == null) {
            throw new RuntimeException("현재 배정된 문제가 없습니다. 먼저 문제를 받으세요.");
        }

        var problem = problemRepository.findById(pid)
                .orElseThrow(() -> new RuntimeException("문제 없음"));

        return userAnswerService.evaluateUserAnswer(user, problem, code, language);
    }
}
