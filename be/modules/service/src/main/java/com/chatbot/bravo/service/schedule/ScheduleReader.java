package com.chatbot.bravo.service.schedule;

import com.chatbot.bravo.infrastructure.schedule.repository.ScheduleRepository;
import com.chatbot.bravo.model.schedule.Schedule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 일정 조회 usecase — turn(대화)과 무관, API·툴 공용 조회 진입점.
 * 소유권은 쿼리 레벨(userId 조건)로 강제되므로 별도 검증 없음.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleReader {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_PERIOD_DAYS = 7;
    private static final int MAX_SIZE = 100;

    private final ScheduleRepository scheduleRepository;

    /**
     * 기간 내 일정을 scheduled_at 역순(최신순)으로 최대 size건 반환.
     *
     * @param from 시작일(포함) — null이면 오늘(KST)
     * @param to   종료일(포함) — null이면 from+6일 (총 7일)
     * @param size 최대 건수 — 1~100으로 클램프
     */
    public List<Schedule> readInPeriod(Long userId, LocalDate from, LocalDate to, int size) {
        LocalDate fromDate = from != null ? from : LocalDate.now(KST);
        LocalDate toDate = to != null ? to : fromDate.plusDays(DEFAULT_PERIOD_DAYS - 1);
        int cappedSize = Math.max(1, Math.min(size, MAX_SIZE));

        Instant f = fromDate.atStartOfDay(KST).toInstant();
        Instant t = toDate.plusDays(1).atStartOfDay(KST).toInstant();   // to는 inclusive

        List<Schedule> ascending = scheduleRepository.findAllByUserIdInPeriod(userId, f, t);
        List<Schedule> descending = new ArrayList<>(ascending);
        Collections.reverse(descending);
        return descending.size() > cappedSize ? List.copyOf(descending.subList(0, cappedSize)) : descending;
    }
}
