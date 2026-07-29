package com.chatbot.bravo.api.schedule;

import com.chatbot.bravo.api.auth.LoginUser;
import com.chatbot.bravo.api.schedule.dto.CreateScheduleRequest;
import com.chatbot.bravo.api.schedule.dto.ScheduleResponse;
import com.chatbot.bravo.api.schedule.dto.SchedulesResponse;
import com.chatbot.bravo.api.schedule.dto.UpdateScheduleRequest;
import com.chatbot.bravo.model.auth.LoginSession;
import com.chatbot.bravo.model.schedule.Schedule;
import com.chatbot.bravo.model.schedule.ScheduleType;
import com.chatbot.bravo.service.schedule.ScheduleReader;
import com.chatbot.bravo.service.schedule.ScheduleWriter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 일정 REST API. 인증된 세션의 userId로만 동작 — 소유권은 usecase/쿼리 레벨에서 강제되고,
 * 타 유저 일정 접근은 404로 존재를 숨긴다.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Schedule", description = "일정 API")
public class ScheduleApiController {

    private final ScheduleReader scheduleReader;
    private final ScheduleWriter scheduleWriter;

    @Operation(summary = "일정 기간 조회 — from/to 생략 시 오늘(KST)부터 7일, to 포함, 최신순, 최대 size건")
    @GetMapping("/schedules")
    public SchedulesResponse getSchedules(
            @LoginUser LoginSession loginSession,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /schedules - from={}, to={}, size={}", from, to, size);
        return SchedulesResponse.from(
                scheduleReader.readInPeriod(loginSession.getUserId(), from, to, size));
    }

    @Operation(summary = "일정 등록 — API 발 생성(turn 없음)")
    @PostMapping("/schedules")
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduleResponse createSchedule(
            @LoginUser LoginSession loginSession,
            @Valid @RequestBody CreateScheduleRequest request) {
        log.info("POST /schedules");
        Schedule saved = scheduleWriter.create(
                loginSession.getUserId(), null, request.getTitle(), request.getContent(),
                ScheduleType.fromOrEtc(request.getScheduleType()), request.getScheduledAt());
        return ScheduleResponse.from(saved);
    }

    @Operation(summary = "일정 변경 — 교체 방식(새 row + 기존 soft delete). 응답의 scheduleId가 바뀐다")
    @PatchMapping("/schedules/{scheduleId}")
    public ScheduleResponse updateSchedule(
            @LoginUser LoginSession loginSession,
            @PathVariable Long scheduleId,
            @Valid @RequestBody UpdateScheduleRequest request) {
        log.info("PATCH /schedules/{}", scheduleId);
        Schedule saved = scheduleWriter.replaceById(
                loginSession.getUserId(), scheduleId, request.getTitle(), request.getContent(),
                request.getScheduleType() != null ? ScheduleType.fromOrEtc(request.getScheduleType()) : null,
                request.getScheduledAt());
        return ScheduleResponse.from(saved);
    }

    @Operation(summary = "일정 삭제 (soft delete)")
    @DeleteMapping("/schedules/{scheduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSchedule(
            @LoginUser LoginSession loginSession,
            @PathVariable Long scheduleId) {
        log.info("DELETE /schedules/{}", scheduleId);
        scheduleWriter.delete(loginSession.getUserId(), scheduleId);
    }
}
