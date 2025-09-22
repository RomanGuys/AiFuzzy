package com.example.aifuzzy.service;

import com.example.aifuzzy.domain.SessionStatus;
import com.example.aifuzzy.dto.RequestExecutionDto;
import com.example.aifuzzy.dto.RequestResultDto;
import com.example.aifuzzy.dto.TestSessionCreateRequestDto;
import com.example.aifuzzy.dto.TestSessionResponseDto;
import com.example.aifuzzy.dto.TestSessionSummaryDto;
import com.example.aifuzzy.http.HttpExecutionResult;
import com.example.aifuzzy.http.HttpRequestExecutor;
import com.example.aifuzzy.llm.GeneratedHttpRequest;
import com.example.aifuzzy.llm.LlmClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.TestConfiguration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpMethod;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TestSessionServiceIntegrationTest {

    private final TestSessionService service;

    TestSessionServiceIntegrationTest(TestSessionService service) {
        this.service = service;
    }

    @Test
    void createsSessionProcessesRequestsAndBuildsStatistics() {
        TestSessionCreateRequestDto request = new TestSessionCreateRequestDto();
        request.setBaseUrl("https://api.example.com");
        request.setSpecification("{\"paths\":{\"/hello\":{\"get\":{\"summary\":\"Hello\"}}}}");
        request.setMaxRequests(5);

        TestSessionResponseDto response = service.createSession(request);

        TestSessionSummaryDto summary = service.getSessionSummary(response.sessionId());
        assertThat(summary.status()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(summary.generatedRequests()).isEqualTo(2);
        assertThat(summary.executedRequests()).isEqualTo(2);
        assertThat(summary.successCount()).isEqualTo(1);
        assertThat(summary.failureCount()).isEqualTo(1);
        assertThat(summary.successRate()).isGreaterThan(0.0);
        assertThat(summary.averageLatencyMillis()).isNotNull();
        assertThat(summary.statusCodeHistogram()).containsEntry(200, 1L).containsEntry(500, 1L);

        List<RequestResultDto> results = service.getSessionResults(response.sessionId());
        assertThat(results).hasSize(2);

        RequestResultDto first = results.get(0);
        assertThat(first.method()).isEqualTo("GET");
        assertThat(first.executions()).hasSize(1);
        RequestExecutionDto firstExecution = first.executions().get(0);
        assertThat(firstExecution.statusCode()).isEqualTo(200);
        assertThat(firstExecution.finalUrl()).isEqualTo("https://api.example.com/hello");
        assertThat(firstExecution.responseBody()).contains("ok");

        RequestResultDto second = results.get(1);
        assertThat(second.method()).isEqualTo("POST");
        assertThat(second.executions()).hasSize(1);
        RequestExecutionDto secondExecution = second.executions().get(0);
        assertThat(secondExecution.statusCode()).isEqualTo(500);
        assertThat(secondExecution.errorMessage()).isEqualTo("Internal error");
    }

    @TestConfiguration
    static class TestBeans {

        @Bean
        public TaskExecutor taskExecutor() {
            return new SyncTaskExecutor();
        }

        @Bean
        @Primary
        public LlmClient llmClient() {
            return new StubLlmClient();
        }

        @Bean
        @Primary
        public HttpRequestExecutor httpRequestExecutor() {
            return new StubHttpRequestExecutor();
        }
    }

    private static class StubLlmClient implements LlmClient {

        @Override
        public List<GeneratedHttpRequest> generateRequests(String specification, int maxRequests) {
            return List.of(
                    new GeneratedHttpRequest(HttpMethod.GET, "/hello", Map.of(), Map.of(), null, "get hello"),
                    new GeneratedHttpRequest(HttpMethod.POST, "/items", Map.of(), Map.of("X-Test", "value"), "{\"name\":\"item\"}", "create item")
            );
        }
    }

    private static class StubHttpRequestExecutor implements HttpRequestExecutor {

        @Override
        public HttpExecutionResult execute(URI baseUri, GeneratedHttpRequest request) {
            URI target = UriComponentsBuilder.fromUri(baseUri)
                    .path(request.path())
                    .build(true)
                    .toUri();
            if (request.method() == HttpMethod.POST) {
                return new HttpExecutionResult(target, 500, "{\"error\":true}", Map.of(), 12L, "Internal error");
            }
            return new HttpExecutionResult(target, 200, "{\"status\":\"ok\"}", Map.of("Content-Type", "application/json"), 5L, null);
        }
    }
}
