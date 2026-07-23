package com.chatbot.bravo.jdbc.auth.repository;

import com.chatbot.bravo.model.audit.AuditFields;
import com.chatbot.bravo.model.auth.User;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("users")
@Getter
public class UserEntity implements AuditFields {

    @Id
    private Long id;
    private String username;
    private String passwordHash;

    private Boolean isDeleted;
    private Instant deletedAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public UserEntity(Long id, String username, String passwordHash,
                      Boolean isDeleted, Instant deletedAt,
                      Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.isDeleted = isDeleted;
        this.deletedAt = deletedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public User toDomain() {
        return new User(id, username, passwordHash, createdAt, updatedAt);
    }

    public static UserEntity from(User domain) {
        return new UserEntity(
                domain.getUserId(), domain.getUsername(), domain.getPassword(),
                false, null,
                domain.getCreatedAt(), domain.getUpdatedAt());
    }

    public UserEntity softDelete() {
        return new UserEntity(id, username, passwordHash,
                true, Instant.now(),
                createdAt, Instant.now());
    }
}
