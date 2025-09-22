package com.example.aifuzzy.dto;

import com.example.aifuzzy.domain.SessionStatus;

import java.util.UUID;

public record TestSessionResponseDto(UUID sessionId, SessionStatus status) {
}
