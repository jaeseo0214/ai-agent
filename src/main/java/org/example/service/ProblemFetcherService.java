// src/main/java/org/example/service/ProblemFetcherService.java
package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.entity.Problem;
import org.example.repository.ProblemRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProblemFetcherService {

    private final ProblemRepository coreRepo;

    /** 난이도(level)에서 무작위 1문제 */
    public Problem fetchFromDbByLevel(int level) {
        return coreRepo.pickRandomSingleLevel(level);
    }
}