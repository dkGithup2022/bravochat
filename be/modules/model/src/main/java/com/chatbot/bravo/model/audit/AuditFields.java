package com.chatbot.bravo.model.audit;

import java.time.Instant;

public interface AuditFields {
    Instant getCreatedAt();
    Instant getUpdatedAt();
}
