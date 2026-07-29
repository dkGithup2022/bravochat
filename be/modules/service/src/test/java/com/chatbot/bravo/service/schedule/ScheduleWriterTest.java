package com.chatbot.bravo.service.schedule;

import com.chatbot.bravo.exception.schedule.ScheduleNotFoundException;
import com.chatbot.bravo.infrastructure.schedule.repository.ScheduleRepository;
import com.chatbot.bravo.model.schedule.Schedule;
import com.chatbot.bravo.model.schedule.ScheduleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** ScheduleWriter — 교체 정책(새 row + soft delete)·null 유지 병합·404 은닉을 검증한다. */
@ExtendWith(MockitoExtension.class)
class ScheduleWriterTest {

    @Mock private ScheduleRepository scheduleRepository;

    @InjectMocks
    private ScheduleWriter writer;

    private static Schedule saved(Long id, Long turnId, String title, ScheduleType type, Instant at) {
        Instant now = Instant.now();
        return new Schedule(id, 7L, turnId, title, "상세", type, at, null, now, now);
    }

    @Test
    @DisplayName("[create] turnId null(API 발 생성)을 그대로 저장한다")
    void should_allowNullTurnId_when_create() {
        when(scheduleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        writer.create(7L, null, "회의", null, ScheduleType.WORK, Instant.parse("2026-07-30T06:00:00Z"));

        ArgumentCaptor<Schedule> captor = ArgumentCaptor.forClass(Schedule.class);
        verify(scheduleRepository).save(captor.capture());
        assertThat(captor.getValue().getTurnId()).isNull();
        assertThat(captor.getValue().getUserId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("[replace] 새 row 저장 → 기존 soft delete 순서, null 파라미터는 기존 값 유지")
    void should_insertThenSoftDelete_when_replace() {
        Schedule old = saved(3L, 50L, "돌돌이 미팅", ScheduleType.ETC, Instant.parse("2026-07-30T10:00:00Z"));
        when(scheduleRepository.save(any())).thenAnswer(inv -> {
            Schedule s = inv.getArgument(0);
            return saved(9L, s.getTurnId(), s.getTitle(), s.getScheduleType(), s.getScheduledAt());
        });

        Schedule result = writer.replace(old, 100L, null, null, null, Instant.parse("2026-07-30T11:00:00Z"));

        ArgumentCaptor<Schedule> captor = ArgumentCaptor.forClass(Schedule.class);
        InOrder inOrder = Mockito.inOrder(scheduleRepository);
        inOrder.verify(scheduleRepository).save(captor.capture());
        inOrder.verify(scheduleRepository).softDelete(3L, 7L);

        Schedule created = captor.getValue();
        assertThat(created.getTurnId()).isEqualTo(100L);                  // 변경 출처 턴
        assertThat(created.getTitle()).isEqualTo("돌돌이 미팅");           // null → 기존 값 유지
        assertThat(created.getScheduleType()).isEqualTo(ScheduleType.ETC);
        assertThat(created.getScheduledAt()).isEqualTo(Instant.parse("2026-07-30T11:00:00Z"));
        assertThat(result.getScheduleId()).isEqualTo(9L);                 // 반환은 새 row
    }

    @Test
    @DisplayName("[replaceById] 소유 일정이면 교체(turnId=null), 아니면 404")
    void should_findAndReplace_when_replaceById() {
        Schedule old = saved(3L, 50L, "미팅", ScheduleType.WORK, Instant.parse("2026-07-30T06:00:00Z"));
        when(scheduleRepository.findByIdAndUserId(3L, 7L)).thenReturn(Optional.of(old));
        when(scheduleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        writer.replaceById(7L, 3L, "새 제목", null, null, null);

        ArgumentCaptor<Schedule> captor = ArgumentCaptor.forClass(Schedule.class);
        verify(scheduleRepository).save(captor.capture());
        assertThat(captor.getValue().getTurnId()).isNull();               // API 발 변경
        assertThat(captor.getValue().getTitle()).isEqualTo("새 제목");
    }

    @Test
    @DisplayName("[replaceById 실패] 타 유저/없는 일정이면 ScheduleNotFoundException(404) — 존재 은닉")
    void should_throw404_when_replaceByIdNotOwned() {
        when(scheduleRepository.findByIdAndUserId(3L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> writer.replaceById(7L, 3L, "새 제목", null, null, null))
                .isInstanceOf(ScheduleNotFoundException.class);
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("[delete] softDelete 0행이면 ScheduleNotFoundException(404)")
    void should_throw404_when_deleteNotOwned() {
        when(scheduleRepository.softDelete(3L, 7L)).thenReturn(false);

        assertThatThrownBy(() -> writer.delete(7L, 3L))
                .isInstanceOf(ScheduleNotFoundException.class);
    }

    @Test
    @DisplayName("[delete 성공] softDelete 1행이면 정상 종료")
    void should_succeed_when_deleteOwned() {
        when(scheduleRepository.softDelete(3L, 7L)).thenReturn(true);

        writer.delete(7L, 3L);

        verify(scheduleRepository).softDelete(3L, 7L);
    }
}
