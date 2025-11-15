package org.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Entity
// 사용자 레벨 업데이트를 위한 getter, setter 추가
@Getter 
@Setter
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

    // 기존의 core.users 테이블을 삭제하고 사용자 레벨까지 users1에서 관리하기 위해 추가
    @Column(nullable = false)
    private Integer level;
}
