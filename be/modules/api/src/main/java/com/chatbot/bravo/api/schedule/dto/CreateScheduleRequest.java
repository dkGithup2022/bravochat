package com.chatbot.bravo.api.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "일정 등록 요청")
public class CreateScheduleRequest {

    @NotBlank
    @Size(max = 200)
    @Schema(description = "제목", example = "강남 미팅")
    private String title;

    @Schema(description = "상세 — 선택")
    private String content;

    @Schema(description = "분류(HEALTH|PERSONAL|WORK|ETC) — 선택, 미스매치는 ETC 흡수", example = "WORK")
    private String scheduleType;

    @NotNull
    @Schema(description = "일정 시각 (UTC ISO-8601) — KST 변환은 클라이언트 책임",
            example = "2026-07-30T06:00:00Z")
    private Instant scheduledAt;
}
