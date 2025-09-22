package com.example.aifuzzy.llm;

import java.util.Locale;

import org.springframework.http.HttpMethod;

/**
 * Simple client stub used to demonstrate HTTP method resolution.
 */
public class SimpleLlmClient {

    /**
     * Resolve an HTTP method name to a {@link HttpMethod} instance.
     * <p>
     * In Spring 6 the {@code HttpMethod.resolve} helper was removed, so we
     * reproduce the previous behaviour by normalising the input and catching
     * the {@link IllegalArgumentException} thrown by {@link HttpMethod#valueOf(String)}.
     * </p>
     *
     * @param methodName raw HTTP method name
     * @return the resolved {@code HttpMethod} or {@code null} when the method is not supported
     */
    protected HttpMethod resolveHttpMethod(String methodName) {
        if (methodName == null) {
            return null;
        }

        String normalized = methodName.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        normalized = normalized.toUpperCase(Locale.ROOT);
        try {
            return HttpMethod.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return null;
        }

    }
}
