package com.example.aifuzzy.service;

import com.example.aifuzzy.domain.GeneratedRequestEntity;
import com.example.aifuzzy.domain.RequestExecutionEntity;
import com.example.aifuzzy.domain.SessionStatus;
import com.example.aifuzzy.domain.TestSessionEntity;
import com.example.aifuzzy.http.HttpExecutionResult;
import com.example.aifuzzy.http.HttpRequestExecutor;
import com.example.aifuzzy.llm.GeneratedHttpRequest;
import com.example.aifuzzy.llm.LlmClient;
import com.example.aifuzzy.repository.GeneratedRequestRepository;
import com.example.aifuzzy.repository.RequestExecutionRepository;
import com.example.aifuzzy.repository.TestSessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class TestSessionProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestSessionProcessor.class);

    private final TestSessionRepository sessionRepository;
    private final GeneratedRequestRepository generatedRequestRepository;
    private final RequestExecutionRepository executionRepository;
    private final LlmClient llmClient;
    private final HttpRequestExecutor httpRequestExecutor;
    private final ObjectMapper objectMapper;

    public TestSessionProcessor(TestSessionRepository sessionRepository,
                                GeneratedRequestRepository generatedRequestRepository,
                                RequestExecutionRepository executionRepository,
                                LlmClient llmClient,
                                HttpRequestExecutor httpRequestExecutor,
                                ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.generatedRequestRepository = generatedRequestRepository;
        this.executionRepository = executionRepository;
        this.llmClient = llmClient;
        this.httpRequestExecutor = httpRequestExecutor;
        this.objectMapper = objectMapper;
    }

    @Async
    public void processSessionAsync(UUID sessionId, int maxRequests) {
        Optional<TestSessionEntity> optional = sessionRepository.findById(sessionId);
        if (optional.isEmpty()) {
            LOGGER.warn("Cannot process session {} because it no longer exists", sessionId);
            return;
        }
        TestSessionEntity session = optional.get();
        try {
            session.setStatus(SessionStatus.PROCESSING);
            session.setUpdatedAt(Instant.now());
            sessionRepository.save(session);

            List<GeneratedHttpRequest> generatedRequests = llmClient.generateRequests(session.getSpecification(), maxRequests);
            int limit = maxRequests > 0 ? Math.min(maxRequests, generatedRequests.size()) : generatedRequests.size();
            URI baseUri = URI.create(session.getBaseUrl());

            for (int i = 0; i < limit; i++) {
                GeneratedHttpRequest generated = generatedRequests.get(i);
                GeneratedRequestEntity requestEntity = createRequestEntity(session, generated);
                generatedRequestRepository.save(requestEntity);

                RequestExecutionEntity executionEntity = executeRequest(requestEntity, baseUri, generated);
                executionRepository.save(executionEntity);
            }

            session.setStatus(SessionStatus.COMPLETED);
            session.setUpdatedAt(Instant.now());
            sessionRepository.save(session);
        } catch (Exception exception) {
            LOGGER.error("Failed to process session {}", sessionId, exception);
            session.setStatus(SessionStatus.FAILED);
            session.setFailureMessage(exception.getMessage());
            session.setUpdatedAt(Instant.now());
            sessionRepository.save(session);
        }
    }

    private GeneratedRequestEntity createRequestEntity(TestSessionEntity session, GeneratedHttpRequest generated) {
        GeneratedRequestEntity entity = new GeneratedRequestEntity();
        entity.setId(UUID.randomUUID());
        entity.setSession(session);
        entity.setHttpMethod(generated.method().name());
        entity.setPath(generated.path());
        entity.setQueryParameters(toJson(generated.queryParameters()));
        entity.setHeaders(toJson(generated.headers()));
        entity.setBody(generated.body());
        entity.setDescription(generated.description());
        return entity;
    }

    private RequestExecutionEntity executeRequest(GeneratedRequestEntity requestEntity, URI baseUri, GeneratedHttpRequest generated) {
        RequestExecutionEntity execution = new RequestExecutionEntity();
        execution.setId(UUID.randomUUID());
        execution.setGeneratedRequest(requestEntity);
        execution.setExecutedAt(Instant.now());
        try {
            HttpExecutionResult result = httpRequestExecutor.execute(baseUri, generated);
            if (result.finalUrl() != null) {
                execution.setFinalUrl(result.finalUrl().toString());
            } else {
                execution.setFinalUrl(baseUri.toString());
            }
            execution.setStatusCode(result.statusCode());
            execution.setResponseBody(result.responseBody());
            execution.setResponseHeaders(toJson(result.responseHeaders()));
            execution.setLatencyMillis(result.latencyMillis());
            execution.setErrorMessage(result.errorMessage());
        } catch (Exception exception) {
            LOGGER.warn("Failed to execute generated request {}", requestEntity.getId(), exception);
            execution.setFinalUrl(baseUri.toString());
            execution.setErrorMessage(exception.getMessage());
        }
        requestEntity.getExecutions().add(execution);
        return execution;
    }

    private String toJson(Map<?, ?> value) {
        if (value == null || value.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialise map", e);
        }
    }
}
