package org.example.controller;

import org.example.entity.Problem;
import org.example.service.ProblemFetcherService;
import org.example.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class ProblemController {
    private final ProblemFetcherService fetcher;
    private final ProblemService problemService;

    @GetMapping("/fetch")
    public ResponseEntity<?> fetch(@RequestParam String url) {
        try {
            Problem p = fetcher.fetchFromBaekjoon(url);
            Problem saved = problemService.save(p);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("fetch error: " + e.getMessage());
        }
    }

    @GetMapping("/recommend")
    public ResponseEntity<?> recommend(@RequestParam String username) {
        Problem p = problemService.recommendForUser(username);
        if (p == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(p);
    }
}