package com.chatbot.bravo.service.chat.agent.tool.schedule;

import com.chatbot.bravo.infrastructure.llm.ToolParamExtractor;
import com.chatbot.bravo.infrastructure.schedule.repository.ScheduleRepository;
import com.chatbot.bravo.model.llm.ToolInvocation;
import com.chatbot.bravo.model.schedule.Schedule;
import com.chatbot.bravo.model.schedule.ScheduleType;
import com.chatbot.bravo.service.chat.agent.tool.ToolContext;
import com.chatbot.bravo.service.chat.agent.tool.ToolResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

    @InjectMocks
    private ScheduleToolHandler handler;

    private static final ToolInvocation CALL = new ToolInvocation("schedule", Map.of());
    private static final ToolContext CTX = new ToolContext(7L, 100L, List.of());

    private void extractorReturns(ScheduleParams params) {
        when(paramExtractor.extract(anyString(), eq(ScheduleParams.class), anyList())).thenReturn(params);
    }

    private static ScheduleParams addParams(String title, String scheduledAt, String type) {
        return new ScheduleParams("add", null, title, null, type, scheduledAt, null, null);
    }

    private static Schedule savedSchedule(Long id, String title, ScheduleType type, Instant at) {
        Instant now = Instant.now();
        return new Schedule(id, 7L, 100L, title, null, type, at, null, now, now);
    }

    // ------------------------------------------------------------------ add

    @Test
    @DisplayName("[add 성공] KST 로컬 시각을 UTC로 변환해 저장하고, userId/turnId를 컨텍스트에서 채운다")
    void should_saveWithUtcAndContextIds_when_add() {
        extractorReturns(addParams("회의", "2026-07-28T15:00", "WORK"));
        when(scheduleRepository.save(any())).thenAnswer(inv -> {
            Schedule s = inv.getArgument(0);
            return savedSchedule(15L, s.getTitle(), s.getScheduleType(), s.getScheduledAt());
        });

        ToolResponse response = handler.handle(CALL, CTX);

        ArgumentCaptor<Schedule> captor = ArgumentCaptor.forClass(Schedule.class);
        verify(scheduleRepository).save(captor.capture());
        Schedule saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(7L);
        assertThat(saved.getTurnId()).isEqualTo(100L);   // 생성 출처 턴
        // KST 15:00 == UTC 06:00
        assertThat(saved.getScheduledAt()).isEqualTo(Instant.parse("2026-07-28T06:00:00Z"));
        assertThat(saved.getScheduleType()).isEqualTo(ScheduleType.WORK);

        assertThat(response.success()).isTrue();
        assertThat(response.response()).contains("등록됨").contains("[15]").contains("회의");
        assertThat(response.turnMemo()).startsWith("schedule.add:");
    }

    @Test
    @DisplayName("[add 경계] 미스매치 scheduleType은 ETC로 흡수된다")
    void should_absorbUnknownType_when_add() {
        extractorReturns(addParams("회의", "2026-07-28T15:00", "밥약속"));
        when(scheduleRepository.save(any())).thenAnswer(inv -> {
            Schedule s = inv.getArgument(0);
            return savedSchedule(1L, s.getTitle(), s.getScheduleType(), s.getScheduledAt());
        });

        handler.handle(CALL, CTX);

        ArgumentCaptor<Schedule> captor = ArgumentCaptor.forClass(Schedule.class);
        verify(scheduleRepository).save(captor.capture());
        assertThat(captor.getValue().getScheduleType()).isEqualTo(ScheduleType.ETC);
    }

    @Test
    @DisplayName("[add 실패] title 없으면 저장 없이 fail")
    void should_failWithoutSave_when_titleMissing() {
        extractorReturns(addParams(null, "2026-07-28T15:00", null));

        ToolResponse response = handler.handle(CALL, CTX);

        assertThat(response.success()).isFalse();
        assertThat(response.response()).contains("title");
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("[add 실패] 시각 형식이 틀리면 형식 힌트를 담아 fail — 모델 자가수정 유도")
    void should_failWithFormatHint_when_badDateTime() {
        extractorReturns(addParams("회의", "내일 3시", null));

        ToolResponse response = handler.handle(CALL, CTX);

        assertThat(response.success()).isFalse();
        assertThat(response.response()).contains("YYYY-MM-DDTHH:mm");
        verify(scheduleRepository, never()).save(any());
    }

    // ----------------------------------------------------------------- list

    @Test
    @DisplayName("[list 성공] from/to를 KST 자정 기준 [from, to)로 변환해 조회한다")
    void should_queryWithKstMidnightRange_when_list() {
        extractorReturns(new ScheduleParams("list", null, null, null, null, null,
                "2026-07-28", "2026-07-30"));
        when(scheduleRepository.findAllByUserIdInPeriod(eq(7L), any(), any())).thenReturn(List.of(
                savedSchedule(1L, "회의", ScheduleType.WORK, Instant.parse("2026-07-28T06:00:00Z"))));

        ToolResponse response = handler.handle(CALL, CTX);

        // KST 2026-07-28 00:00 == UTC 2026-07-27 15:00
        verify(scheduleRepository).findAllByUserIdInPeriod(7L,
                Instant.parse("2026-07-27T15:00:00Z"), Instant.parse("2026-07-29T15:00:00Z"));
        assertThat(response.success()).isTrue();
        assertThat(response.response()).contains("일정 1건").contains("[1]").contains("회의");
        assertThat(response.turnMemo()).isEqualTo("schedule.list: 1건 (2026-07-28 ~ 2026-07-30)");
    }

    @Test
    @DisplayName("[list 경계] 캡(20건) 초과분은 '외 n건'으로 잘라 노출한다")
    void should_capAt20_when_manySchedules() {
        extractorReturns(new ScheduleParams("list", null, null, null, null, null,
                "2026-07-28", "2026-08-28"));
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
        extractorReturns(new ScheduleParams("list", null, null, null, null, null, null, null));
        when(scheduleRepository.findAllByUserIdInPeriod(eq(7L), any(), any())).thenReturn(List.of());

        ToolResponse response = handler.handle(CALL, CTX);

        assertThat(response.success()).isTrue();
        assertThat(response.response()).contains("일정 없음");
    }

    // -------------------------------------------------------------- missing/기타

    @Test
    @DisplayName("[missing] 정보 부족이면 추출기가 만든 질문을 fail로 되먹인다")
    void should_failWithQuestion_when_missing() {
        extractorReturns(new ScheduleParams("missing", "몇 시로 잡을까요?", null, null, null, null, null, null));

        ToolResponse response = handler.handle(CALL, CTX);

        assertThat(response.success()).isFalse();
        assertThat(response.response()).isEqualTo("몇 시로 잡을까요?");
    }

    @Test
    @DisplayName("[실패] 알 수 없는 op면 fail")
    void should_fail_when_unknownOp() {
        extractorReturns(new ScheduleParams("delete", null, null, null, null, null, null, null));

        ToolResponse response = handler.handle(CALL, CTX);

        assertThat(response.success()).isFalse();
        assertThat(response.response()).contains("unknown schedule op");
    }
}
