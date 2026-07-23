package com.chatbot.bravo.api.chat.dto;

import com.chatbot.bravo.service.chat.dto.SendMessageResult;

import java.time.Instant;

public record SendMessageResponse(
        Long turnId,
        String message,
        Instant createdAt
) {
    public static SendMessageResponse from(SendMessageResult result) {
        return new SendMessageResponse(result.turnId(), result.message(), result.createdAt());
    }
}
