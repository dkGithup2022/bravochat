package com.chatbot.bravo.api.chat.dto;

import com.chatbot.bravo.model.chat.RecentTurn;

import java.time.Instant;

public record RecentTurnResponse(
        Long turnId,
        String userMessage,
        String assistantMessage,
        Instant createdAt
) {
    public static RecentTurnResponse from(RecentTurn turn) {
        return new RecentTurnResponse(
                turn.getTurnId(),
                turn.getUserMessage(),
                turn.getAssistantMessage(),
                turn.getCreatedAt());
    }
}
