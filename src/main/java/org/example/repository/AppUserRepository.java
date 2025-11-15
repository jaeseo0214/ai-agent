package org.example.repository;

import org.example.entity.LogInEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<LogInEntity, Long> {
}
