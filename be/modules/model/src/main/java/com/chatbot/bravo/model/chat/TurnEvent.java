package com.chatbot.bravo.model.chat;

import com.chatbot.bravo.model.audit.AuditFields;
import lombok.Value;

import java.time.Instant;

/**
 * Turn 내부에서 발생한 메시지/툴 실행 기록. append 순서 = id(AUTO_INCREMENT) 오름차순으로 보장.
 * 팩토리로 type별 필드 사용규약(도메인 스펙 §4)을 캡슐화한다.
 */
@Value
public class TurnEvent implements AuditFields {
    Long eventId;
    Long turnId;
    TurnEventType type;
    String content;
    String toolName;
    String toolCallId;
    Instant createdAt;
    Instant updatedAt;

    /** 사용자 입력. toolName/toolCallId 미사용. */
    public static TurnEvent userMessage(Long turnId, String content) {
        Instant now = Instant.now();
        return new TurnEvent(null, turnId, TurnEventType.USER_MESSAGE, content, null, null, now, now);
    }

    /** 최종 응답. toolName/toolCallId 미사용. */
    public static TurnEvent assistantMessage(Long turnId, String content) {
        Instant now = Instant.now();
        return new TurnEvent(null, turnId, TurnEventType.ASSISTANT_MESSAGE, content, null, null, now, now);
    }

    /** LLM 툴 호출 요청. content=툴 인자(JSON). */
    public static TurnEvent toolCall(Long turnId, String toolName, String toolCallId, String content) {
        Instant now = Instant.now();
        return new TurnEvent(null, turnId, TurnEventType.TOOL_CALL, content, toolName, toolCallId, now, now);
    }

    /** 툴 실행 중간 기록(디버깅용). 대응 TOOL_CALL과 동일 toolCallId, content=중간 정리 결과. */
    public static TurnEvent toolProgress(Long turnId, String toolName, String toolCallId, String content) {
        Instant now = Instant.now();
        return new TurnEvent(null, turnId, TurnEventType.TOOL_PROGRESS, content, toolName, toolCallId, now, now);
    }

    /** 툴 실행 결과. 대응 TOOL_CALL과 동일 toolCallId, content=결과(JSON). */
    public static TurnEvent toolResult(Long turnId, String toolCallId, String content) {
        Instant now = Instant.now();
        return new TurnEvent(null, turnId, TurnEventType.TOOL_RESULT, content, null, toolCallId, now, now);
    }
}
