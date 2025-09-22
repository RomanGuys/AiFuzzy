package com.example.aifuzzy.dto;

import com.example.aifuzzy.domain.SessionStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record TestSessionSummaryDto(
        UUID sessionId,
        SessionStatus status,
        int generatedRequests,
        int executedRequests,
        int successCount,
        int failureCount,
        double successRate,
        Long averageLatencyMillis,
        Map<Integer, Long> statusCodeHistogram,
        Instant createdAt,
        Instant updatedAt,
        String failureMessage
) {
}
