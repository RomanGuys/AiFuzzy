package com.example.aifuzzy.http;

import java.net.URI;
import java.util.Map;

public record HttpExecutionResult(
        URI finalUrl,
        Integer statusCode,
        String responseBody,
        Map<String, String> responseHeaders,
        Long latencyMillis,
        String errorMessage
) {
}
