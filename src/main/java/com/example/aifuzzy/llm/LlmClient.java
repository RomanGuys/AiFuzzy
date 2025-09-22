package com.example.aifuzzy.llm;

import java.util.List;

public interface LlmClient {

    List<GeneratedHttpRequest> generateRequests(String specification, int maxRequests);
}
