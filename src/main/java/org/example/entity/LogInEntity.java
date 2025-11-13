package org.example.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "users1") // PostgreSQL의 users 테이블과 매핑
@Data
public class LogInEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id", nullable = false, unique = true)
    private String loginId;

    @Column(name = "login_pw", nullable = false)
    private String loginPw;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;
}
