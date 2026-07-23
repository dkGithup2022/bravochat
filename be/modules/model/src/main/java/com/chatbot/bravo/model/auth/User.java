package com.chatbot.bravo.model.auth;

import com.chatbot.bravo.model.audit.AuditFields;
import lombok.Value;

import java.time.Instant;

@Value
public class User implements AuditFields {
    Long userId;
    String username;
    String password;
    Instant createdAt;
    Instant updatedAt;

    public static User create(String username, String password) {
        Instant now = Instant.now();
        return new User(null, username, password, now, now);
    }
}
