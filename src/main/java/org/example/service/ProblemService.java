package org.example.service;

import org.example.entity.AppUser;
import org.example.entity.Problem;
import org.example.repository.AppUserRepository;
import org.example.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final AppUserRepository userRepository;
    private final Random random = new Random();

    public Problem save(Problem problem) {
        return problemRepository.save(problem);
    }

    /**
     * 사용자 기준으로 문제 추천
     * - 사용자의 level을 받아서 난이도 범위 결정
     * - 동일 레벨 문제가 없으면 모든 문제 중에서 랜덤으로 제공
     */
    public Problem recommendForUser(String username) {
        Optional<AppUser> user = userRepository.findByUsername(username);
        int level = (user.isPresent() && user.get().getLevel() != null) ? user.get().getLevel() : 1;

        // 간단 매핑: level 1~2 -> level=1, 3~4->2, 5->3 (DB에 맞게 세분화 가능)
        int dbLevel = Math.max(1, Math.min(5, level)); // 보정
        List<Problem> list = problemRepository.findByLevel(dbLevel);
        if (list == null || list.isEmpty()) {
            List<Problem> all = problemRepository.findAll();
            if (all.isEmpty()) return null;
            return all.get(random.nextInt(all.size()));
        }
        return list.get(random.nextInt(list.size()));
    }
}