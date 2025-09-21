package com.example.aifuzzy.http;

import com.example.aifuzzy.llm.GeneratedHttpRequest;

import java.net.URI;

public interface HttpRequestExecutor {

    HttpExecutionResult execute(URI baseUri, GeneratedHttpRequest request);
}
