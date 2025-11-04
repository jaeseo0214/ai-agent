package org.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "users", schema = "core")  // ← core.users 에 매핑
public class AppUser {

    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "level", nullable = false)
    private Integer level;               // 정수 레벨(예: 1)

    @CreationTimestamp
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
