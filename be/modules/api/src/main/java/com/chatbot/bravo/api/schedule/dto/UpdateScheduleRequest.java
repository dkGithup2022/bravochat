package com.chatbot.bravo.api.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "일정 변경 요청 — 온 필드만 변경(null은 기존 값 유지)")
public class UpdateScheduleRequest {

    @Size(max = 200)
    @Schema(description = "새 제목 — 선택")
    private String title;

    @Schema(description = "새 상세 — 선택")
    private String content;

    @Schema(description = "새 분류(HEALTH|PERSONAL|WORK|ETC) — 선택", example = "PERSONAL")
    private String scheduleType;

    @Schema(description = "새 일정 시각 (UTC ISO-8601) — 선택", example = "2026-07-30T11:00:00Z")
    private Instant scheduledAt;
}
