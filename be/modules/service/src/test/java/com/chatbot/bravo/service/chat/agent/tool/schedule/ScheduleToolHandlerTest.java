package com.chatbot.bravo.service.chat.agent.tool.schedule;

import com.chatbot.bravo.infrastructure.llm.ToolParamExtractor;
import com.chatbot.bravo.infrastructure.schedule.repository.ScheduleRepository;
import com.chatbot.bravo.model.llm.ToolInvocation;
import com.chatbot.bravo.model.schedule.Schedule;
import com.chatbot.bravo.model.schedule.ScheduleType;
import com.chatbot.bravo.service.chat.agent.tool.ToolContext;
import com.chatbot.bravo.service.chat.agent.tool.ToolResponse;
import com.chatbot.bravo.service.schedule.ScheduleWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ScheduleToolHandler 단위 테스트 — 추출기(ToolParamExtractor)를 모킹해
 * op 디스패치·검증·KST→UTC 변환·조회 정책(기본기간/캡)을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class ScheduleToolHandlerTest {

    @Mock private ToolParamExtractor paramExtractor;
    @Mock private ScheduleRepository scheduleRepository;
    @Mock private ScheduleWriter scheduleWriter;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ScheduleToolHandler handler;

    private static final ToolInvocation CALL = new ToolInvocation("schedule", Map.of());
    private static final ToolContext CTX = new ToolContext(7L, 100L, List.of());

    private void extractorReturns(ScheduleParams params) {
        when(paramExtractor.extract(anyString(), eq(ScheduleParams.class), anyList())).thenReturn(params);
    }

    private static ScheduleParams addParams(String title, String scheduledAt, String type) {
        return new ScheduleParams("add", null, title, null, type, scheduledAt, null, null, null, null, null);
    }

    private static Schedule savedSchedule(Long id, String title, ScheduleType type, Instant at) {
        Instant now = Instant.now();
        return new Schedule(id, 7L, 100L, title, null, type, at, null, now, now);
    }

    // ------------------------------------------------------------------ add

    @Test
    @DisplayName("[add 성공] KST 로컬 시각을 UTC로 변환해 Writer.create에 위임하고, userId/turnId를 컨텍스트에서 채운다")
    void should_saveWithUtcAndContextIds_when_add() {
        extractorReturns(addParams("회의", "2026-07-28T15:00", "WORK"));
        when(scheduleWriter.create(any(), any(), any(), any(), any(), any())).thenAnswer(inv ->
                savedSchedule(15L, inv.getArgument(2), inv.getArgument(4), inv.getArgument(5)));

        ToolResponse response = handler.handle(CALL, CTX);

        // KST 15:00 == UTC 06:00, 생성 출처 턴 = ctx.turnId
        verify(scheduleWriter).create(7L, 100L, "회의", null,
                ScheduleType.WORK, Instant.parse("2026-07-28T06:00:00Z"));

        assertThat(response.success()).isTrue();
        assertThat(response.response()).contains("등록됨").contains("[15]").contains("회의");
        // turnMemo = JSON 래퍼 {tool, stage, success, params, memo, returned}
        assertThat(response.turnMemo())
                .contains("\"tool\":\"schedule\"")
                .contains("\"stage\":\"EXECUTE\"")
                .contains("\"success\":true")
                .contains("\"op\":\"add\"")          // 추출된 params가 기록됨
                .contains("schedule.add:");          // 툴 원본 memo 보존
    }

    @Test
    @DisplayName("[add 경계] 미스매치 scheduleType은 ETC로 흡수된다")
    void should_absorbUnknownType_when_add() {
        extractorReturns(addParams("회의", "2026-07-28T15:00", "밥약속"));
        when(scheduleWriter.create(any(), any(), any(), any(), any(), any())).thenAnswer(inv ->
                savedSchedule(1L, inv.getArgument(2), inv.getArgument(4), inv.getArgument(5)));

        handler.handle(CALL, CTX);

        verify(scheduleWriter).create(eq(7L), eq(100L), eq("회의"), isNull(),
                eq(ScheduleType.ETC), any());
    }

    @Test
    @DisplayName("[add 실패] title 없으면 저장 없이 fail")
    void should_failWithoutSave_when_titleMissing() {
        extractorReturns(addParams(null, "2026-07-28T15:00", null));

        ToolResponse response = handler.handle(CALL, CTX);

        assertThat(response.success()).isFalse();
        assertThat(response.response()).contains("title");
        verify(scheduleWriter, never()).create(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("[add 실패] 시각 형식이 틀리면 형식 힌트를 담아 fail — 모델 자가수정 유도")
    void should_failWithFormatHint_when_badDateTime() {
        extractorReturns(addParams("회의", "내일 3시", null));

        ToolResponse response = handler.handle(CALL, CTX);

        assertThat(response.success()).isFalse();
        assertThat(response.response()).contains("YYYY-MM-DDTHH:mm");
        verify(scheduleWriter, never()).create(any(), any(), any(), any(), any(), any());
    }

    // ----------------------------------------------------------------- list

    @Test
    @DisplayName("[list 성공] from/to를 KST 자정 기준으로 변환해 조회한다 — to는 그 날짜까지 포함")
    void should_queryWithKstMidnightRange_when_list() {
        extractorReturns(new ScheduleParams("list", null, null, null, null, null,
                "2026-07-28", "2026-07-30", null, null, null));
        when(scheduleRepository.findAllByUserIdInPeriod(eq(7L), any(), any())).thenReturn(List.of(
                savedSchedule(1L, "회의", ScheduleType.WORK, Instant.parse("2026-07-28T06:00:00Z"))));

        ToolResponse response = handler.handle(CALL, CTX);

        // KST 2026-07-28 00:00 == UTC 2026-07-27 15:00 / to(7-30) 포함 → 7-31 00:00 KST 직전까지
        verify(scheduleRepository).findAllByUserIdInPeriod(7L,
                Instant.parse("2026-07-27T15:00:00Z"), Instant.parse("2026-07-30T15:00:00Z"));
        assertThat(response.success()).isTrue();
        assertThat(response.response()).contains("일정 1건").contains("[1]").contains("회의");
        assertThat(response.turnMemo()).contains("schedule.list: 1건 (2026-07-28 ~ 2026-07-30)");
    }

    @Test
    @DisplayName("[list 경계] from=to(하루 조회)면 그 날짜 전체가 구간이 된다 — 빈 구간 회귀 방지")
    void should_coverWholeDay_when_fromEqualsTo() {
        extractorReturns(new ScheduleParams("list", null, null, null, null, null,
                "2026-07-30", "2026-07-30", null, null, null));
        when(scheduleRepository.findAllByUserIdInPeriod(eq(7L), any(), any())).thenReturn(List.of(
                savedSchedule(1L, "미팅", ScheduleType.WORK, Instant.parse("2026-07-30T06:00:00Z"))));

        ToolResponse response = handler.handle(CALL, CTX);

        // KST 7-30 00:00 ~ 7-31 00:00 == UTC 7-29 15:00 ~ 7-30 15:00
        verify(scheduleRepository).findAllByUserIdInPeriod(7L,
                Instant.parse("2026-07-29T15:00:00Z"), Instant.parse("2026-07-30T15:00:00Z"));
        assertThat(response.response()).contains("일정 1건").contains("미팅");
    }

    @Test
    @DisplayName("[list 경계] 캡(20건) 초과분은 '외 n건'으로 잘라 노출한다")
    void should_capAt20_when_manySchedules() {
        extractorReturns(new ScheduleParams("list", null, null, null, null, null,
                "2026-07-28", "2026-08-28", null, null, null));
        List<Schedule> many = IntStream.rangeClosed(1, 25)
                .mapToObj(i -> savedSchedule((long) i, "일정" + i, ScheduleType.PERSONAL,
                        Instant.parse("2026-07-28T06:00:00Z").plusSeconds(i * 3600L)))
                .toList();
        when(scheduleRepository.findAllByUserIdInPeriod(eq(7L), any(), any())).thenReturn(many);

        ToolResponse response = handler.handle(CALL, CTX);

        assertThat(response.response())
                .contains("일정 25건")
                .contains("[20]")
                .doesNotContain("[21]")
                .contains("외 5건");
    }

    @Test
    @DisplayName("[list 경계] 결과 없으면 '없음' 응답 — 모델이 지어내지 않게 명시")
    void should_sayEmpty_when_noSchedules() {
        extractorReturns(new ScheduleParams("list", null, null, null, null, null, null, null, null, null, null));
        when(scheduleRepository.findAllByUserIdInPeriod(eq(7L), any(), any())).thenReturn(List.of());

        ToolResponse response = handler.handle(CALL, CTX);

        assertThat(response.success()).isTrue();
        assertThat(response.response()).contains("일정 없음");
    }

    // --------------------------------------------------------------- update

    /** update 1단계 파라미터 — 시각 변경 요청. */
    private static ScheduleParams updateParams(String targetTitle, String targetDate, String newScheduledAt) {
        return new ScheduleParams("update", null, null, null, null, newScheduledAt, null, null,
                targetTitle, targetDate, null);
    }

    @Test
    @DisplayName("[update 1단계] 후보 1건이면 DB 변경 없이 변경 전→후를 담은 확인 제안을 리턴한다")
    void should_proposeConfirmWithoutChange_when_updateSingleCandidate() {
        extractorReturns(updateParams("돌돌이 미팅", "2026-07-30", "2026-07-30T20:00"));
        // KST 19:00 == UTC 10:00
        when(scheduleRepository.findAllByUserIdInPeriod(eq(7L), any(), any())).thenReturn(List.of(
                savedSchedule(3L, "돌돌이 미팅", ScheduleType.ETC, Instant.parse("2026-07-30T10:00:00Z"))));

        ToolResponse response = handler.handle(CALL, CTX);

        assertThat(response.success()).isTrue();
        assertThat(response.response())
                .contains("한 번에 1건씩")              // 일괄 변경 불가 안내 — 모델이 유저에게 재확인
                .contains("변경 확인 필요")
                .contains("19:00").contains("20:00")   // 변경 전·후 시각이 모두 노출 — 2단계 추출 근거
                .contains("돌돌이 미팅")
                .doesNotContain("[3]");                // 내부 ID 비노출
        verify(scheduleWriter, never()).replace(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("[update 1단계] 후보가 여러 건이면 시각 포함 목록을 fail로 되먹여 유저가 고르게 한다")
    void should_failWithCandidates_when_updateAmbiguous() {
        extractorReturns(updateParams("미팅", null, "2026-07-30T20:00"));
        when(scheduleRepository.findAllByUserIdInPeriod(eq(7L), any(), any())).thenReturn(List.of(
                savedSchedule(1L, "강남 미팅", ScheduleType.WORK, Instant.parse("2026-07-30T06:00:00Z")),
                savedSchedule(3L, "돌돌이 미팅", ScheduleType.ETC, Instant.parse("2026-07-30T10:00:00Z"))));

        ToolResponse response = handler.handle(CALL, CTX);

        assertThat(response.success()).isFalse();
        assertThat(response.response()).contains("여러 건")
                .contains("15:00").contains("강남 미팅")
                .contains("19:00").contains("돌돌이 미팅");
        verify(scheduleWriter, never()).replace(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("[update 1단계] 대상을 못 찾으면 검색 범위를 담아 fail")
    void should_failWithRange_when_updateTargetNotFound() {
        extractorReturns(updateParams("없는 일정", "2026-07-30", "2026-07-30T20:00"));
        when(scheduleRepository.findAllByUserIdInPeriod(eq(7L), any(), any())).thenReturn(List.of());

        ToolResponse response = handler.handle(CALL, CTX);

        assertThat(response.success()).isFalse();
        assertThat(response.response()).contains("찾지 못했습니다").contains("없는 일정");
        verify(scheduleWriter, never()).replace(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("[apply_update 성공] 대상 특정 후 Writer.replace에 위임 — 교체 정책은 Writer 소유")
    void should_insertNewAndSoftDeleteOld_when_applyUpdate() {
        extractorReturns(new ScheduleParams("apply_update", null, null, null, null,
                "2026-07-30T20:00", null, null, "돌돌이 미팅", null, "2026-07-30T19:00"));
        Schedule old = savedSchedule(3L, "돌돌이 미팅", ScheduleType.ETC, Instant.parse("2026-07-30T10:00:00Z"));
        when(scheduleRepository.findAllByUserIdInPeriod(eq(7L), any(), any())).thenReturn(List.of(old));
        when(scheduleWriter.replace(any(), any(), any(), any(), any(), any())).thenReturn(
                savedSchedule(9L, "돌돌이 미팅", ScheduleType.ETC, Instant.parse("2026-07-30T11:00:00Z")));

        ToolResponse response = handler.handle(CALL, CTX);

        // 변경 출처 턴 = 100L, 시각만 변경(KST 20:00 == UTC 11:00), 나머지 null = 기존 값 유지
        ArgumentCaptor<Schedule> oldCaptor = ArgumentCaptor.forClass(Schedule.class);
        verify(scheduleWriter).replace(oldCaptor.capture(), eq(100L), isNull(), isNull(), isNull(),
                eq(Instant.parse("2026-07-30T11:00:00Z")));
        assertThat(oldCaptor.getValue().getScheduleId()).isEqualTo(3L);

        assertThat(response.success()).isTrue();
        assertThat(response.response()).contains("변경됨").contains("19:00").contains("20:00");
    }

    @Test
    @DisplayName("[apply_update 실패] 변경 전 시각이 안 맞으면 적용하지 않고 1단계 재진행 유도")
    void should_failWithoutApply_when_targetTimeMismatch() {
        extractorReturns(new ScheduleParams("apply_update", null, null, null, null,
                "2026-07-30T20:00", null, null, "돌돌이 미팅", null, "2026-07-30T18:00"));
        // 저장된 일정은 KST 19:00 — targetScheduledAt(18:00)과 불일치
        when(scheduleRepository.findAllByUserIdInPeriod(eq(7L), any(), any())).thenReturn(List.of(
                savedSchedule(3L, "돌돌이 미팅", ScheduleType.ETC, Instant.parse("2026-07-30T10:00:00Z"))));

        ToolResponse response = handler.handle(CALL, CTX);

        assertThat(response.success()).isFalse();
        assertThat(response.response()).contains("찾지 못했습니다").contains("다시");
        verify(scheduleWriter, never()).replace(any(), any(), any(), any(), any(), any());
    }

    // -------------------------------------------------------------- missing/기타

    @Test
    @DisplayName("[missing] 정보 부족이면 추출기가 만든 질문을 fail로 되먹인다")
    void should_failWithQuestion_when_missing() {
        extractorReturns(new ScheduleParams("missing", "몇 시로 잡을까요?", null, null, null, null, null, null, null, null, null));

        ToolResponse response = handler.handle(CALL, CTX);

        assertThat(response.success()).isFalse();
        assertThat(response.response()).isEqualTo("몇 시로 잡을까요?");
    }

    @Test
    @DisplayName("[실패] 알 수 없는 op면 fail")
    void should_fail_when_unknownOp() {
        extractorReturns(new ScheduleParams("delete", null, null, null, null, null, null, null, null, null, null));

        ToolResponse response = handler.handle(CALL, CTX);

        assertThat(response.success()).isFalse();
        assertThat(response.response()).contains("unknown schedule op");
    }
}
