package com.example.aifuzzy.llm;

import org.springframework.http.HttpMethod;

import java.util.Map;

public record GeneratedHttpRequest(
        HttpMethod method,
        String path,
        Map<String, Object> queryParameters,
        Map<String, String> headers,
        String body,
        String description
) {
}
