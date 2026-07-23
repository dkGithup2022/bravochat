package com.chatbot.bravo.model.chat;

import com.chatbot.bravo.model.audit.AuditFields;
import lombok.Value;

import java.time.Instant;

/**
 * 사용자 요청 1회 ~ 최종 응답까지의 실행 단위. 메시지 본문은 TurnEvent에 보관.
 */
@Value
public class Turn implements AuditFields {
    Long turnId;
    Long userId;
    TurnStatus status;
    Instant completedAt;
    String failureReason;
    Instant createdAt;
    Instant updatedAt;

    /** 처리 시작 — PROCESSING 상태로 신규 생성. */
    public static Turn start(Long userId) {
        Instant now = Instant.now();
        return new Turn(null, userId, TurnStatus.PROCESSING, null, null, now, now);
    }

    /** 최종 응답 저장 완료 — COMPLETED. */
    public Turn complete() {
        Instant now = Instant.now();
        return new Turn(turnId, userId, TurnStatus.COMPLETED, now, failureReason, createdAt, now);
    }

    /** 처리 중 오류 — FAILED + 실패 사유. */
    public Turn fail(String reason) {
        Instant now = Instant.now();
        return new Turn(turnId, userId, TurnStatus.FAILED, now, reason, createdAt, now);
    }
}
