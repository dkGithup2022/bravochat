package com.chatbot.bravo.api.chat.dto;

import com.chatbot.bravo.service.chat.dto.GetRecentTurnsResult;

import java.util.List;

public record RecentTurnsResponse(
        List<RecentTurnResponse> turns
) {
    public static RecentTurnsResponse from(GetRecentTurnsResult result) {
        List<RecentTurnResponse> turns = result.turns().stream()
                .map(RecentTurnResponse::from)
                .toList();
        return new RecentTurnsResponse(turns);
    }
}
