package com.example.aifuzzy.http;

/**
 * Executes HTTP requests using a web client. The executor decides whether a
 * particular HTTP method supports a request body via {@link #allowsBody(HttpMethod)}.
 */
public class WebClientHttpRequestExecutor {

    /**
     * Determine if a request body is allowed for the supplied HTTP method.
     *
     * @param method HTTP method to check.
     * @return {@code true} when a request body is allowed, {@code false} otherwise.
     */
    public boolean allowsBody(HttpMethod method) {
        if (method == null) {
            return false;
        }

        if (method == HttpMethod.GET) {
            return false;
        } else if (method == HttpMethod.HEAD) {
            return false;
        } else if (method == HttpMethod.OPTIONS) {
            return false;
        } else if (method == HttpMethod.TRACE) {
            return false;
        } else {
            return true;
        }
    }
}
