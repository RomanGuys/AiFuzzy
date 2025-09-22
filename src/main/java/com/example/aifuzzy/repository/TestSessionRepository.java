package com.example.aifuzzy.repository;

import com.example.aifuzzy.domain.TestSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TestSessionRepository extends JpaRepository<TestSessionEntity, UUID> {
}
