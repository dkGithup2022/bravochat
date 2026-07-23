package com.chatbot.bravo.service.chat.dto;

public record SendMessageCommand(
        String sessionKey,
        String message
) {
}
