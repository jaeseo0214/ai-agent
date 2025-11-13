package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.UserLogInDto;
import org.example.dto.UserSignUpDto;
import org.example.entity.LogInEntity;
import org.example.repository.UserLogInRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserLogInService {
    private final UserLogInRepository userLoginRepository;
    private final PasswordEncoder passwordEncoder;

    public void register(UserSignUpDto dto) {
        if(userLoginRepository.findByLoginId(dto.getLoginId()).isPresent()){
            throw new RuntimeException("이미 존재하는 아이디 입니다.");
        }

        LogInEntity user = new LogInEntity(); // ✅ 변경됨
        user.setLoginId(dto.getLoginId());
        user.setLoginPw(passwordEncoder.encode(dto.getLoginPw())); // 비밀번호 암호화
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());

        userLoginRepository.save(user);
    }

    public LogInEntity login(UserLogInDto dto) {
        LogInEntity user = userLoginRepository.findByLoginId(dto.getLoginId())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 아이디입니다."));

        if (!passwordEncoder.matches(dto.getLoginPw(), user.getLoginPw())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        return user;
    }
}
