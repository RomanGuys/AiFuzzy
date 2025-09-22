package com.example.aifuzzy.http;

import com.example.aifuzzy.llm.GeneratedHttpRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class WebClientHttpRequestExecutor implements HttpRequestExecutor {

    private final WebClient webClient;

    public WebClientHttpRequestExecutor(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    @Override
    public HttpExecutionResult execute(URI baseUri, GeneratedHttpRequest request) {
        URI targetUri = buildUri(baseUri, request);
        HttpHeaders headers = new HttpHeaders();
        if (request.headers() != null) {
            request.headers().forEach(headers::add);
        }
        boolean hasBody = request.body() != null && allowsBody(request.method());
        if (hasBody && headers.getContentType() == null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }

        Instant startedAt = Instant.now();
        try {
            WebClient.RequestBodySpec bodySpec = webClient.method(request.method())
                    .uri(targetUri)
                    .headers(h -> h.addAll(headers));

            WebClient.RequestHeadersSpec<?> headersSpec = bodySpec;
            if (hasBody) {
                headersSpec = bodySpec.bodyValue(request.body());
            }

            ResponseEntity<String> responseEntity = headersSpec.exchangeToMono(response -> response.toEntity(String.class))
                    .block();

            Instant finishedAt = Instant.now();
            long latency = Duration.between(startedAt, finishedAt).toMillis();
            Map<String, String> responseHeaders = flattenHeaders(responseEntity.getHeaders());
            return new HttpExecutionResult(
                    targetUri,
                    responseEntity.getStatusCode().value(),
                    responseEntity.getBody(),
                    responseHeaders,
                    latency,
                    null
            );
        } catch (WebClientResponseException exception) {
            Instant finishedAt = Instant.now();
            long latency = Duration.between(startedAt, finishedAt).toMillis();
            Map<String, String> responseHeaders = flattenHeaders(exception.getHeaders());
            return new HttpExecutionResult(
                    targetUri,
                    exception.getRawStatusCode(),
                    exception.getResponseBodyAsString(),
                    responseHeaders,
                    latency,
                    exception.getMessage()
            );
        } catch (WebClientRequestException exception) {
            Instant finishedAt = Instant.now();
            long latency = Duration.between(startedAt, finishedAt).toMillis();
            return new HttpExecutionResult(
                    targetUri,
                    null,
                    null,
                    Map.of(),
                    latency,
                    exception.getMessage()
            );
        }
    }

    private URI buildUri(URI baseUri, GeneratedHttpRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUri(baseUri);
        if (StringUtils.hasText(request.path())) {
            builder.path(request.path());
        }
        MultiValueMap<String, String> queryParams = buildQueryParameters(request.queryParameters());
        if (!queryParams.isEmpty()) {
            builder.queryParams(queryParams);
        }
        return builder.build(true).toUri();
    }

    private MultiValueMap<String, String> buildQueryParameters(Map<String, Object> parameters) {
        MultiValueMap<String, String> result = new LinkedMultiValueMap<>();
        if (parameters == null) {
            return result;
        }
        parameters.forEach((key, value) -> {
            if (value == null) {
                return;
            }
            if (value instanceof Iterable<?> iterable) {
                for (Object item : iterable) {
                    result.add(key, Objects.toString(item));
                }
            } else if (value.getClass().isArray()) {
                int length = java.lang.reflect.Array.getLength(value);
                for (int i = 0; i < length; i++) {
                    Object item = java.lang.reflect.Array.get(value, i);
                    result.add(key, Objects.toString(item));
                }
            } else {
                result.add(key, Objects.toString(value));
            }
        });
        return result;
    }

    private Map<String, String> flattenHeaders(HttpHeaders headers) {
        if (headers == null) {
            return Map.of();
        }
        Map<String, String> flattened = new HashMap<>();
        headers.forEach((key, values) -> {
            if (!values.isEmpty()) {
                flattened.put(key, String.join(", ", values));
            }
        });
        return flattened;
    }

    private boolean allowsBody(HttpMethod method) {
        return switch (method) {
            case GET, HEAD, OPTIONS, TRACE -> false;
            default -> true;
        };
    }
}
