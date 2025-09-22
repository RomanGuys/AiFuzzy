package com.example.aifuzzy.repository;

import com.example.aifuzzy.domain.GeneratedRequestEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GeneratedRequestRepository extends JpaRepository<GeneratedRequestEntity, UUID> {

    long countBySession_Id(UUID sessionId);

    @EntityGraph(attributePaths = "executions")
    List<GeneratedRequestEntity> findAllBySession_IdOrderByPathAsc(UUID sessionId);
}
