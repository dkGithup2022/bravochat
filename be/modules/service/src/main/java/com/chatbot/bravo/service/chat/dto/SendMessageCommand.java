package com.chatbot.bravo.service.chat.dto;

public record SendMessageCommand(
        Long userId,
        String message
) {
}
