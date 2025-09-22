package com.example.aifuzzy.repository;

import com.example.aifuzzy.domain.RequestExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RequestExecutionRepository extends JpaRepository<RequestExecutionEntity, UUID> {

    long countByGeneratedRequest_Session_Id(UUID sessionId);

    List<RequestExecutionEntity> findAllByGeneratedRequest_Session_Id(UUID sessionId);
}
