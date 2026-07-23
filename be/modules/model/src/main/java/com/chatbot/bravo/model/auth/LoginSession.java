package com.chatbot.bravo.model.auth;

import com.chatbot.bravo.model.audit.AuditFields;
import lombok.Value;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Value
public class LoginSession implements AuditFields {

    private static final Duration SESSION_TTL = Duration.ofDays(7);

    Long loginSessionId;
    String sessionKey;
    Long userId;
    Instant lastLoggedInAt;
    Instant lastRequestedAt;
    Instant createdAt;
    Instant updatedAt;

    public static LoginSession issue(Long userId) {
        Instant now = Instant.now();
        return new LoginSession(null, UUID.randomUUID().toString(), userId, now, now, now, now);
    }

    public LoginSession touch() {
        Instant now = Instant.now();
        return new LoginSession(loginSessionId, sessionKey, userId, lastLoggedInAt, now, createdAt, now);
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(lastLoggedInAt.plus(SESSION_TTL));
    }
}
