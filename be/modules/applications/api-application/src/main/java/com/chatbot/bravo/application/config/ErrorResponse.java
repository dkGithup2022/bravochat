package com.chatbot.bravo.application.config;

public record ErrorResponse(
        int status,
        String error,
        String message
) {}
