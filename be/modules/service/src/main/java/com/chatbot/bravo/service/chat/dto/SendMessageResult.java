package com.chatbot.bravo.service.chat.dto;

import java.time.Instant;

public record SendMessageResult(
        Long turnId,
        String message,
        Instant createdAt
) {
}
