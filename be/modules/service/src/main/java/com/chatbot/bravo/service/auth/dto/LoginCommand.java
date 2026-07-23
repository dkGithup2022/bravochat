package com.chatbot.bravo.service.auth.dto;

public record LoginCommand(
        String username,
        String rawPassword
) {
}
