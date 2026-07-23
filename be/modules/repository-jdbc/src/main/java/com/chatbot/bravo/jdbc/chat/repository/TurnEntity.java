package com.chatbot.bravo.jdbc.chat.repository;

import com.chatbot.bravo.model.audit.AuditFields;
import com.chatbot.bravo.model.chat.Turn;
import com.chatbot.bravo.model.chat.TurnStatus;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("turns")
@Getter
public class TurnEntity implements AuditFields {

    @Id
    private Long id;
    private Long userId;
    private TurnStatus status;
    private Instant completedAt;
    private String failureReason;

    private Boolean isDeleted;
    private Instant deletedAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public TurnEntity(Long id, Long userId, TurnStatus status, Instant completedAt, String failureReason,
                      Boolean isDeleted, Instant deletedAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.status = status;
        this.completedAt = completedAt;
        this.failureReason = failureReason;
        this.isDeleted = isDeleted;
        this.deletedAt = deletedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Turn toDomain() {
        return new Turn(id, userId, status, completedAt, failureReason, createdAt, updatedAt);
    }

    public static TurnEntity from(Turn domain) {
        return new TurnEntity(
                domain.getTurnId(), domain.getUserId(), domain.getStatus(),
                domain.getCompletedAt(), domain.getFailureReason(),
                false, null,
                domain.getCreatedAt(), domain.getUpdatedAt());
    }

    public TurnEntity softDelete() {
        return new TurnEntity(id, userId, status, completedAt, failureReason,
                true, Instant.now(), createdAt, Instant.now());
    }
}
