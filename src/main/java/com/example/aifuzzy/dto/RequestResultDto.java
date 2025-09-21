package com.example.aifuzzy.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RequestResultDto(
        UUID requestId,
        String method,
        String path,
        Map<String, Object> queryParameters,
        Map<String, String> headers,
        String body,
        String description,
        List<RequestExecutionDto> executions
) {
}
