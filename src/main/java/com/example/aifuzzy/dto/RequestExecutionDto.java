package com.example.aifuzzy.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record RequestExecutionDto(
        UUID executionId,
        String finalUrl,
        Instant executedAt,
        Integer statusCode,
        Long latencyMillis,
        Map<String, String> responseHeaders,
        String responseBody,
        String errorMessage
) {
}
