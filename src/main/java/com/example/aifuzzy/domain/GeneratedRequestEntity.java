package com.example.aifuzzy.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "generated_requests")
public class GeneratedRequestEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private TestSessionEntity session;

    @Column(name = "http_method", nullable = false)
    private String httpMethod;

    @Column(name = "path", nullable = false)
    private String path;

    @Lob
    @Column(name = "query_parameters")
    private String queryParameters;

    @Lob
    @Column(name = "headers")
    private String headers;

    @Lob
    @Column(name = "body")
    private String body;

    @Lob
    @Column(name = "description")
    private String description;

    @OneToMany(mappedBy = "generatedRequest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RequestExecutionEntity> executions = new ArrayList<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public TestSessionEntity getSession() {
        return session;
    }

    public void setSession(TestSessionEntity session) {
        this.session = session;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getQueryParameters() {
        return queryParameters;
    }

    public void setQueryParameters(String queryParameters) {
        this.queryParameters = queryParameters;
    }

    public String getHeaders() {
        return headers;
    }

    public void setHeaders(String headers) {
        this.headers = headers;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<RequestExecutionEntity> getExecutions() {
        return executions;
    }

    public void setExecutions(List<RequestExecutionEntity> executions) {
        this.executions = executions;
    }
}
