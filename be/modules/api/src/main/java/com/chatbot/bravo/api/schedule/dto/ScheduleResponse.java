package com.chatbot.bravo.api.schedule.dto;

import com.chatbot.bravo.model.schedule.Schedule;
import com.chatbot.bravo.model.schedule.ScheduleType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "일정 응답")
public record ScheduleResponse(
        @Schema(description = "일정 ID (변경 시 새 ID로 바뀜)", example = "1") Long scheduleId,
        @Schema(description = "제목", example = "강남 미팅") String title,
        @Schema(description = "상세 (없으면 null)") String content,
        @Schema(description = "분류") ScheduleType scheduleType,
        @Schema(description = "일정 시각 (UTC ISO-8601)", example = "2026-07-30T06:00:00Z") Instant scheduledAt,
        @Schema(description = "완료 여부") boolean done
) {
    public static ScheduleResponse from(Schedule schedule) {
        return new ScheduleResponse(
                schedule.getScheduleId(),
                schedule.getTitle(),
                schedule.getContent(),
                schedule.getScheduleType(),
                schedule.getScheduledAt(),
                schedule.isDone()
        );
    }
}
