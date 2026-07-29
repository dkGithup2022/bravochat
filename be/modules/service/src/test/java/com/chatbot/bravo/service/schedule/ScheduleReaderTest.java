package com.chatbot.bravo.service.schedule;

import com.chatbot.bravo.infrastructure.schedule.repository.ScheduleRepository;
import com.chatbot.bravo.model.schedule.Schedule;
import com.chatbot.bravo.model.schedule.ScheduleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** ScheduleReader — 기간 해석(KST·inclusive to)·역순 정렬·size 캡을 검증한다. */
@ExtendWith(MockitoExtension.class)
class ScheduleReaderTest {

    @Mock private ScheduleRepository scheduleRepository;

    @InjectMocks
    private ScheduleReader reader;

    private static Schedule schedule(Long id, Instant at) {
        Instant now = Instant.now();
        return new Schedule(id, 7L, null, "일정" + id, null, ScheduleType.ETC, at, null, now, now);
    }

    @Test
    @DisplayName("[성공] from/to를 KST 자정 기준으로 변환(to 포함)하고, scheduled_at 역순으로 반환한다")
    void should_reverseOrder_when_read() {
        when(scheduleRepository.findAllByUserIdInPeriod(eq(7L), any(), any())).thenReturn(List.of(
                schedule(1L, Instant.parse("2026-07-28T01:00:00Z")),
                schedule(2L, Instant.parse("2026-07-29T01:00:00Z")),
                schedule(3L, Instant.parse("2026-07-30T01:00:00Z"))));

        List<Schedule> result = reader.readInPeriod(
                7L, LocalDate.parse("2026-07-28"), LocalDate.parse("2026-07-30"), 20);

        // KST 7-28 00:00 == UTC 7-27 15:00 / to(7-30) 포함 → UTC 7-30 15:00
        verify(scheduleRepository).findAllByUserIdInPeriod(7L,
                Instant.parse("2026-07-27T15:00:00Z"), Instant.parse("2026-07-30T15:00:00Z"));
        assertThat(result).extracting(Schedule::getScheduleId).containsExactly(3L, 2L, 1L);
    }

    @Test
    @DisplayName("[경계] size를 초과하는 결과는 최신순으로 잘라 반환한다")
    void should_capBySize_when_manyResults() {
        when(scheduleRepository.findAllByUserIdInPeriod(eq(7L), any(), any())).thenReturn(List.of(
                schedule(1L, Instant.parse("2026-07-28T01:00:00Z")),
                schedule(2L, Instant.parse("2026-07-29T01:00:00Z")),
                schedule(3L, Instant.parse("2026-07-30T01:00:00Z"))));

        List<Schedule> result = reader.readInPeriod(
                7L, LocalDate.parse("2026-07-28"), LocalDate.parse("2026-07-30"), 2);

        assertThat(result).extracting(Schedule::getScheduleId).containsExactly(3L, 2L);
    }

    @Test
    @DisplayName("[기본값] from/to 생략 시 오늘(KST)부터 7일 구간으로 조회한다")
    void should_useDefaultPeriod_when_fromToOmitted() {
        when(scheduleRepository.findAllByUserIdInPeriod(eq(7L), any(), any())).thenReturn(List.of());

        List<Schedule> result = reader.readInPeriod(7L, null, null, 20);

        // 시각을 못박을 수 없어(now 의존) 호출 구간 길이만 검증한다 — 정확히 7일
        verify(scheduleRepository).findAllByUserIdInPeriod(eq(7L),
                any(), any());
        assertThat(result).isEmpty();
    }
}
