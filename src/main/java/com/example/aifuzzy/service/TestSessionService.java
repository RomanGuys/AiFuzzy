package com.example.aifuzzy.service;

import com.example.aifuzzy.domain.GeneratedRequestEntity;
import com.example.aifuzzy.domain.RequestExecutionEntity;
import com.example.aifuzzy.domain.SessionStatus;
import com.example.aifuzzy.domain.TestSessionEntity;
import com.example.aifuzzy.dto.RequestExecutionDto;
import com.example.aifuzzy.dto.RequestResultDto;
import com.example.aifuzzy.dto.TestSessionCreateRequestDto;
import com.example.aifuzzy.dto.TestSessionResponseDto;
import com.example.aifuzzy.dto.TestSessionSummaryDto;
import com.example.aifuzzy.exception.ResourceNotFoundException;
import com.example.aifuzzy.repository.GeneratedRequestRepository;
import com.example.aifuzzy.repository.RequestExecutionRepository;
import com.example.aifuzzy.repository.TestSessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TestSessionService {

    private static final int DEFAULT_MAX_REQUESTS = 10;

    private final TestSessionRepository sessionRepository;
    private final GeneratedRequestRepository generatedRequestRepository;
    private final RequestExecutionRepository executionRepository;
    private final TestSessionProcessor processor;
    private final ObjectMapper objectMapper;

    public TestSessionService(TestSessionRepository sessionRepository,
                              GeneratedRequestRepository generatedRequestRepository,
                              RequestExecutionRepository executionRepository,
                              TestSessionProcessor processor,
                              ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.generatedRequestRepository = generatedRequestRepository;
        this.executionRepository = executionRepository;
        this.processor = processor;
        this.objectMapper = objectMapper;
    }

    public TestSessionResponseDto createSession(TestSessionCreateRequestDto requestDto) {
        URI baseUri = validateBaseUrl(requestDto.getBaseUrl());
        String specification = requestDto.getSpecification();
        if (!StringUtils.hasText(specification)) {
            throw new IllegalArgumentException("Specification must not be blank");
        }
        int maxRequests = requestDto.getMaxRequests() != null ? requestDto.getMaxRequests() : DEFAULT_MAX_REQUESTS;

        TestSessionEntity session = new TestSessionEntity();
        session.setId(UUID.randomUUID());
        session.setBaseUrl(baseUri.toString());
        session.setSpecification(specification);
        session.setStatus(SessionStatus.PROCESSING);
        session.setCreatedAt(Instant.now());
        session.setUpdatedAt(Instant.now());
        session.setMaxRequests(maxRequests);
        sessionRepository.save(session);

        processor.processSessionAsync(session.getId(), maxRequests);
        return new TestSessionResponseDto(session.getId(), session.getStatus());
    }

    @Transactional(readOnly = true)
    public TestSessionSummaryDto getSessionSummary(UUID sessionId) {
        TestSessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        long generatedCount = generatedRequestRepository.countBySession_Id(sessionId);
        List<RequestExecutionEntity> executions = executionRepository.findAllByGeneratedRequest_Session_Id(sessionId);
        long executedCount = executions.size();
        long successCount = executions.stream()
                .filter(execution -> execution.getStatusCode() != null
                        && execution.getStatusCode() >= 200
                        && execution.getStatusCode() < 400
                        && !StringUtils.hasText(execution.getErrorMessage()))
                .count();
        long failureCount = executedCount - successCount;
        double successRate = executedCount == 0 ? 0.0 : (double) successCount / executedCount;
        Long averageLatency = executions.stream()
                .map(RequestExecutionEntity::getLatencyMillis)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .average()
                .stream()
                .mapToLong(value -> Math.round(value))
                .boxed()
                .findFirst()
                .orElse(null);

        Map<Integer, Long> histogram = executions.stream()
                .filter(execution -> execution.getStatusCode() != null)
                .collect(Collectors.groupingBy(RequestExecutionEntity::getStatusCode, TreeMap::new, Collectors.counting()));

        return new TestSessionSummaryDto(
                session.getId(),
                session.getStatus(),
                (int) generatedCount,
                (int) executedCount,
                (int) successCount,
                (int) failureCount,
                successRate,
                averageLatency,
                histogram,
                session.getCreatedAt(),
                session.getUpdatedAt(),
                session.getFailureMessage()
        );
    }

    @Transactional(readOnly = true)
    public List<RequestResultDto> getSessionResults(UUID sessionId) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new ResourceNotFoundException("Session not found: " + sessionId);
        }
        List<GeneratedRequestEntity> requests = generatedRequestRepository.findAllBySession_IdOrderByPathAsc(sessionId);
        return requests.stream()
                .map(this::toRequestResultDto)
                .toList();
    }

    private RequestResultDto toRequestResultDto(GeneratedRequestEntity entity) {
        Map<String, Object> queryParameters = readObjectMap(entity.getQueryParameters());
        Map<String, String> headers = readStringMap(entity.getHeaders());
        List<RequestExecutionDto> executions = entity.getExecutions().stream()
                .sorted(Comparator.comparing(RequestExecutionEntity::getExecutedAt))
                .map(this::toExecutionDto)
                .toList();
        return new RequestResultDto(
                entity.getId(),
                entity.getHttpMethod(),
                entity.getPath(),
                queryParameters,
                headers,
                entity.getBody(),
                entity.getDescription(),
                executions
        );
    }

    private RequestExecutionDto toExecutionDto(RequestExecutionEntity execution) {
        Map<String, String> responseHeaders = readStringMap(execution.getResponseHeaders());
        return new RequestExecutionDto(
                execution.getId(),
                execution.getFinalUrl(),
                execution.getExecutedAt(),
                execution.getStatusCode(),
                execution.getLatencyMillis(),
                responseHeaders,
                execution.getResponseBody(),
                execution.getErrorMessage()
        );
    }

    private Map<String, Object> readObjectMap(String json) {
        if (!StringUtils.hasText(json) || "{}".equals(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() { });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse stored JSON", e);
        }
    }

    private Map<String, String> readStringMap(String json) {
        if (!StringUtils.hasText(json) || "{}".equals(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() { });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse stored JSON", e);
        }
    }

    private URI validateBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalArgumentException("Base URL must not be blank");
        }
        URI uri;
        try {
            uri = URI.create(baseUrl);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Base URL is not a valid URI", exception);
        }
        if (!StringUtils.hasText(uri.getScheme())) {
            throw new IllegalArgumentException("Base URL must contain scheme");
        }
        if (!StringUtils.hasText(uri.getHost()) && uri.getAuthority() == null) {
            throw new IllegalArgumentException("Base URL must contain host");
        }
        return uri;
    }
}
