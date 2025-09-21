package com.example.aifuzzy.controller;

import com.example.aifuzzy.dto.RequestResultDto;
import com.example.aifuzzy.dto.TestSessionCreateRequestDto;
import com.example.aifuzzy.dto.TestSessionResponseDto;
import com.example.aifuzzy.dto.TestSessionSummaryDto;
import com.example.aifuzzy.service.TestSessionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/test-sessions")
public class TestSessionController {

    private final TestSessionService testSessionService;

    public TestSessionController(TestSessionService testSessionService) {
        this.testSessionService = testSessionService;
    }

    @PostMapping
    public ResponseEntity<TestSessionResponseDto> createSession(@Valid @RequestBody TestSessionCreateRequestDto requestDto) {
        TestSessionResponseDto responseDto = testSessionService.createSession(requestDto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(responseDto.sessionId())
                .toUri();
        return ResponseEntity.accepted().location(location).body(responseDto);
    }

    @GetMapping("/{sessionId}")
    public TestSessionSummaryDto getSessionSummary(@PathVariable UUID sessionId) {
        return testSessionService.getSessionSummary(sessionId);
    }

    @GetMapping("/{sessionId}/results")
    public List<RequestResultDto> getSessionResults(@PathVariable UUID sessionId) {
        return testSessionService.getSessionResults(sessionId);
    }
}
