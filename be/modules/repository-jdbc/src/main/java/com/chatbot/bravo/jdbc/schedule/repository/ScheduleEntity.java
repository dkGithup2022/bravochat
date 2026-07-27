package com.chatbot.bravo.jdbc.schedule.repository;

import com.chatbot.bravo.model.audit.AuditFields;
import com.chatbot.bravo.model.schedule.Schedule;
import com.chatbot.bravo.model.schedule.ScheduleType;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("schedules")
@Getter
public class ScheduleEntity implements AuditFields {

    @Id
    private Long id;
    private Long userId;
    private Long turnId;
    private String title;
    private String content;
    private ScheduleType scheduleType;
    private Instant scheduledAt;
    private Instant doneAt;

    private Boolean isDeleted;
    private Instant deletedAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public ScheduleEntity(Long id, Long userId, Long turnId, String title, String content,
                          ScheduleType scheduleType, Instant scheduledAt, Instant doneAt,
                          Boolean isDeleted, Instant deletedAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.turnId = turnId;
        this.title = title;
        this.content = content;
        this.scheduleType = scheduleType;
        this.scheduledAt = scheduledAt;
        this.doneAt = doneAt;
        this.isDeleted = isDeleted;
        this.deletedAt = deletedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Schedule toDomain() {
        return new Schedule(id, userId, turnId, title, content, scheduleType, scheduledAt, doneAt,
                createdAt, updatedAt);
    }

    public static ScheduleEntity from(Schedule domain) {
        return new ScheduleEntity(
                domain.getScheduleId(), domain.getUserId(), domain.getTurnId(),
                domain.getTitle(), domain.getContent(), domain.getScheduleType(),
                domain.getScheduledAt(), domain.getDoneAt(),
                false, null,
                domain.getCreatedAt(), domain.getUpdatedAt());
    }

    public ScheduleEntity softDelete() {
        return new ScheduleEntity(id, userId, turnId, title, content, scheduleType, scheduledAt, doneAt,
                true, Instant.now(), createdAt, Instant.now());
    }
}
