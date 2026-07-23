package com.chatbot.bravo.model.chat;

import lombok.Value;

import java.time.Instant;

/**
 * 최근 대화 조회용 읽기 프로젝션.
 * 한 Turn의 USER_MESSAGE + 최종 ASSISTANT_MESSAGE를 agg한 결과 (중간 TOOL_* 제외).
 */
@Value
public class RecentTurn {
    Long turnId;
    String userMessage;
    String assistantMessage;
    Instant createdAt;
}
