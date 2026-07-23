package com.chatbot.bravo.jdbc.auth.repository;

import com.chatbot.bravo.model.audit.AuditFields;
import com.chatbot.bravo.model.auth.LoginSession;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("login_sessions")
@Getter
public class LoginSessionEntity implements AuditFields {

    @Id
    private Long id;
    private String sessionKey;
    private Long userId;
    private Instant lastLoggedInAt;
    private Instant lastRequestedAt;

    private Boolean isDeleted;
    private Instant deletedAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public LoginSessionEntity(Long id, String sessionKey, Long userId,
                              Instant lastLoggedInAt, Instant lastRequestedAt,
                              Boolean isDeleted, Instant deletedAt,
                              Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.sessionKey = sessionKey;
        this.userId = userId;
        this.lastLoggedInAt = lastLoggedInAt;
        this.lastRequestedAt = lastRequestedAt;
        this.isDeleted = isDeleted;
        this.deletedAt = deletedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public LoginSession toDomain() {
        return new LoginSession(id, sessionKey, userId, lastLoggedInAt, lastRequestedAt,
                createdAt, updatedAt);
    }

    public static LoginSessionEntity from(LoginSession domain) {
        return new LoginSessionEntity(
                domain.getLoginSessionId(), domain.getSessionKey(), domain.getUserId(),
                domain.getLastLoggedInAt(), domain.getLastRequestedAt(),
                false, null,
                domain.getCreatedAt(), domain.getUpdatedAt());
    }

    public LoginSessionEntity softDelete() {
        return new LoginSessionEntity(id, sessionKey, userId,
                lastLoggedInAt, lastRequestedAt,
                true, Instant.now(),
                createdAt, Instant.now());
    }
}
