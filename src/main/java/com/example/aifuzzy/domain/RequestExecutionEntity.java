package com.example.aifuzzy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "request_executions")
public class RequestExecutionEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_request_id", nullable = false)
    private GeneratedRequestEntity generatedRequest;

    @Column(name = "final_url", nullable = false)
    private String finalUrl;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "latency_millis")
    private Long latencyMillis;

    @Lob
    @Column(name = "response_body")
    private String responseBody;

    @Lob
    @Column(name = "response_headers")
    private String responseHeaders;

    @Lob
    @Column(name = "error_message")
    private String errorMessage;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public GeneratedRequestEntity getGeneratedRequest() {
        return generatedRequest;
    }

    public void setGeneratedRequest(GeneratedRequestEntity generatedRequest) {
        this.generatedRequest = generatedRequest;
    }

    public String getFinalUrl() {
        return finalUrl;
    }

    public void setFinalUrl(String finalUrl) {
        this.finalUrl = finalUrl;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(Instant executedAt) {
        this.executedAt = executedAt;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public Long getLatencyMillis() {
        return latencyMillis;
    }

    public void setLatencyMillis(Long latencyMillis) {
        this.latencyMillis = latencyMillis;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public String getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(String responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
