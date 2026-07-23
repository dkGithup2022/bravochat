package com.chatbot.bravo.service.chat.dto;

import com.chatbot.bravo.model.chat.RecentTurn;

import java.util.List;

public record GetRecentTurnsResult(
        List<RecentTurn> turns
) {
}
