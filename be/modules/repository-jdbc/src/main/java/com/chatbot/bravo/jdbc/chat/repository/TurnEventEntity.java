package com.chatbot.bravo.jdbc.chat.repository;

import com.chatbot.bravo.model.audit.AuditFields;
import com.chatbot.bravo.model.chat.TurnEvent;
import com.chatbot.bravo.model.chat.TurnEventType;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("turn_events")
@Getter
public class TurnEventEntity implements AuditFields {

    @Id
    private Long id;
    private Long turnId;
    private Integer seq;              // 도메인 TurnEvent.sequence ↔ 컬럼 seq (예약어 sequence 회피)
    private TurnEventType type;
    private String content;
    private String toolName;
    private String toolCallId;

    private Boolean isDeleted;
    private Instant deletedAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public TurnEventEntity(Long id, Long turnId, Integer seq, TurnEventType type, String content,
                           String toolName, String toolCallId,
                           Boolean isDeleted, Instant deletedAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.turnId = turnId;
        this.seq = seq;
        this.type = type;
        this.content = content;
        this.toolName = toolName;
        this.toolCallId = toolCallId;
        this.isDeleted = isDeleted;
        this.deletedAt = deletedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public TurnEvent toDomain() {
        return new TurnEvent(id, turnId, seq, type, content, toolName, toolCallId, createdAt, updatedAt);
    }

    public static TurnEventEntity from(TurnEvent domain) {
        return new TurnEventEntity(
                domain.getEventId(), domain.getTurnId(), domain.getSequence(), domain.getType(),
                domain.getContent(), domain.getToolName(), domain.getToolCallId(),
                false, null,
                domain.getCreatedAt(), domain.getUpdatedAt());
    }

    public TurnEventEntity softDelete() {
        return new TurnEventEntity(id, turnId, seq, type, content, toolName, toolCallId,
                true, Instant.now(), createdAt, Instant.now());
    }
}
