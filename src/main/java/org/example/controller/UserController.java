// 사용자 로그인 시 레벨을 받아와서 드롭다운에 해당 레벨이 선택되어 있게 하는 컨트롤러
package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.entity.LogInEntity;
import org.example.repository.AppUserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    private final AppUserRepository appUserRepository;

    @GetMapping("/{id}/level")
    public ResponseEntity<Integer> getUserLevel(@PathVariable Long id) {
        Optional<LogInEntity> userOpt = appUserRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        LogInEntity user = userOpt.get();
        Integer level = user.getLevel();
        if (level == null) level = 1;  // 기본 1레벨
        return ResponseEntity.ok(level);
    }
}
