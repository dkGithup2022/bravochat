package com.chatbot.bravo.model.chat;

import lombok.Value;

import java.time.Instant;

/**
 * 디버그 전문(transcript) 조회용 읽기 프로젝션.
 * 유저의 모든 Turn(상태 무관)의 모든 이벤트를 시간순으로 나열한 한 줄.
 */
@Value
public class TranscriptEvent {
    Long turnId;
    String turnStatus;
    String eventType;
    String toolName;      // TOOL_CALL/TOOL_RESULT 외에는 null
    String content;
    Instant createdAt;    // 이벤트 생성 시각
}
