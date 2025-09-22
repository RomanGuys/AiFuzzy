package com.example.aifuzzy.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class SimpleLlmClient implements LlmClient {

    private static final List<String> METHOD_ORDER = List.of("get", "post", "put", "delete", "patch");

    private final ObjectMapper objectMapper;

    public SimpleLlmClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public List<GeneratedHttpRequest> generateRequests(String specification, int maxRequests) {
        if (!StringUtils.hasText(specification) || maxRequests <= 0) {
            return List.of();
        }

        List<GeneratedHttpRequest> requests = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(specification);
            JsonNode paths = root.path("paths");
            if (!paths.isObject()) {
                return fallback(specification, maxRequests);
            }

            Iterator<String> pathNames = paths.fieldNames();
            while (pathNames.hasNext() && requests.size() < maxRequests) {
                String pathKey = pathNames.next();
                JsonNode pathNode = paths.path(pathKey);
                List<JsonNode> pathParameters = readParameters(pathNode.path("parameters"));

                for (String methodName : METHOD_ORDER) {
                    if (requests.size() >= maxRequests) {
                        break;
                    }
                    JsonNode methodNode = pathNode.path(methodName);
                    if (!methodNode.isObject()) {
                        continue;
                    }
                    List<JsonNode> methodParameters = readParameters(methodNode.path("parameters"));
                    GeneratedHttpRequest request = buildRequest(pathKey, methodName, pathParameters, methodParameters, methodNode);
                    if (request != null) {
                        requests.add(request);
                    }
                }
            }
        } catch (JsonProcessingException exception) {
            return fallback(specification, maxRequests);
        }

        if (requests.isEmpty()) {
            return fallback(specification, maxRequests);
        }
        return requests;
    }

    private GeneratedHttpRequest buildRequest(String rawPath,
                                              String methodName,
                                              List<JsonNode> pathParameters,
                                              List<JsonNode> methodParameters,
                                              JsonNode methodNode) {
        Map<String, Object> queryParameters = new LinkedHashMap<>();
        Map<String, String> headerParameters = new LinkedHashMap<>();
        Map<String, String> pathParameterValues = new LinkedHashMap<>();

        List<JsonNode> allParameters = new ArrayList<>(pathParameters);
        allParameters.addAll(methodParameters);
        for (JsonNode parameterNode : allParameters) {
            if (!parameterNode.isObject()) {
                continue;
            }
            String location = parameterNode.path("in").asText();
            String name = parameterNode.path("name").asText();
            if (!StringUtils.hasText(location) || !StringUtils.hasText(name)) {
                continue;
            }
            Object sample = sampleValue(parameterNode);
            switch (location) {
                case "query" -> {
                    if (sample != null) {
                        queryParameters.put(name, sample);
                    }
                }
                case "header" -> {
                    if (sample != null) {
                        headerParameters.put(name, String.valueOf(sample));
                    }
                }
                case "path" -> {
                    String value = sample != null ? String.valueOf(sample) : name;
                    pathParameterValues.put(name, value);
                }
                default -> {
                    // ignore other locations for now
                }
            }
        }

        String resolvedPath = resolvePath(rawPath, pathParameterValues);
        HttpMethod httpMethod = HttpMethod.resolve(methodName.toUpperCase(Locale.ROOT));
        if (httpMethod == null) {
            return null;
        }
        String description = methodNode.path("summary").asText(null);
        if (!StringUtils.hasText(description)) {
            description = methodNode.path("description").asText(null);
        }
        String body = extractRequestBody(methodNode.path("requestBody"));

        return new GeneratedHttpRequest(httpMethod, resolvedPath, queryParameters, headerParameters, body, description);
    }

    private List<JsonNode> readParameters(JsonNode node) {
        List<JsonNode> parameters = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(parameters::add);
        }
        return parameters;
    }

    private Object sampleValue(JsonNode parameterNode) {
        JsonNode candidate = firstNonNull(parameterNode.path("example"), parameterNode.path("default"));
        JsonNode schema = parameterNode.path("schema");
        if (schema.isObject()) {
            candidate = firstNonNull(candidate, schema.path("example"), schema.path("default"));
            if (!schema.path("type").isMissingNode() && (candidate == null || candidate.isMissingNode() || candidate.isNull())) {
                return defaultByType(schema.path("type").asText());
            }
        }
        if (candidate != null && !candidate.isMissingNode() && !candidate.isNull()) {
            return convertNode(candidate);
        }
        if (schema.isObject()) {
            return defaultByType(schema.path("type").asText(null));
        }
        return "sample";
    }

    private JsonNode firstNonNull(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            if (node != null && !node.isMissingNode() && !node.isNull()) {
                return node;
            }
        }
        return null;
    }

    private Object convertNode(JsonNode node) {
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isNumber()) {
            return node.numberValue();
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        if (node.isArray()) {
            return objectMapper.convertValue(node, new TypeReference<List<Object>>() { });
        }
        if (node.isObject()) {
            return objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() { });
        }
        return node.asText();
    }

    private Object defaultByType(String type) {
        if (!StringUtils.hasText(type)) {
            return "sample";
        }
        return switch (type) {
            case "integer", "number" -> 1;
            case "boolean" -> true;
            default -> "sample";
        };
    }

    private String resolvePath(String rawPath, Map<String, String> pathParameters) {
        String resolved = rawPath;
        if (!StringUtils.hasText(resolved)) {
            resolved = "/";
        }
        for (Map.Entry<String, String> entry : pathParameters.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String encoded = UriUtils.encodePathSegment(entry.getValue(), StandardCharsets.UTF_8);
            resolved = resolved.replace(placeholder, encoded);
        }
        if (!resolved.startsWith("/")) {
            resolved = "/" + resolved;
        }
        return resolved;
    }

    private String extractRequestBody(JsonNode requestBodyNode) {
        if (!requestBodyNode.isObject()) {
            return null;
        }
        JsonNode content = requestBodyNode.path("content");
        if (!content.isObject()) {
            return null;
        }
        JsonNode candidate = content.path("application/json");
        if (!candidate.isObject()) {
            Iterator<JsonNode> elements = content.elements();
            if (elements.hasNext()) {
                candidate = elements.next();
            }
        }
        if (!candidate.isObject()) {
            return null;
        }
        JsonNode example = firstNonNull(candidate.path("example"), extractExampleFromExamples(candidate.path("examples")));
        if (example == null || example.isMissingNode() || example.isNull()) {
            JsonNode schema = candidate.path("schema");
            example = firstNonNull(schema.path("example"), schema.path("default"));
        }
        if (example == null || example.isMissingNode() || example.isNull()) {
            return null;
        }
        if (example.isTextual()) {
            return example.asText();
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(example);
        } catch (JsonProcessingException e) {
            return example.toString();
        }
    }

    private JsonNode extractExampleFromExamples(JsonNode examplesNode) {
        if (!examplesNode.isObject()) {
            return null;
        }
        Iterator<JsonNode> iterator = examplesNode.elements();
        while (iterator.hasNext()) {
            JsonNode node = iterator.next();
            JsonNode value = node.path("value");
            if (!value.isMissingNode() && !value.isNull()) {
                return value;
            }
        }
        return null;
    }

    private List<GeneratedHttpRequest> fallback(String specification, int maxRequests) {
        String snippet = specification.strip();
        if (snippet.length() > 200) {
            snippet = snippet.substring(0, 200) + "...";
        }
        GeneratedHttpRequest request = new GeneratedHttpRequest(
                HttpMethod.GET,
                "/",
                Map.of(),
                Map.of(),
                null,
                "Fallback request generated from specification snippet: " + snippet
        );
        return List.of(request);
    }
}
