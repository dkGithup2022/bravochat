package com.chatbot.bravo.service.chat.dto;

public record GetRecentTurnsQuery(
        Long userId,
        int size
) {
}
