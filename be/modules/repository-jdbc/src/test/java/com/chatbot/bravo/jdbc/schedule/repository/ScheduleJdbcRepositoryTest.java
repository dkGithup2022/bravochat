package com.chatbot.bravo.jdbc.schedule.repository;

import com.chatbot.bravo.infrastructure.schedule.repository.ScheduleRepository;
import com.chatbot.bravo.model.schedule.Schedule;
import com.chatbot.bravo.model.schedule.ScheduleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJdbcTest
@ComponentScan(basePackages = "com.chatbot.bravo.jdbc.schedule.repository")
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class ScheduleJdbcRepositoryTest {

    private static final Instant BASE = Instant.parse("2026-07-28T00:00:00Z");

    @Autowired
    private ScheduleRepository scheduleRepository;          // 테스트 대상 (인터페이스)

    @Autowired
    private ScheduleEntityRepository scheduleEntityRepository;  // soft-delete 픽스처용

    private Schedule schedule(Long userId, String title, Instant scheduledAt) {
        return scheduleRepository.save(
                Schedule.create(userId, 1L, title, null, ScheduleType.PERSONAL, scheduledAt));
    }

    @Test
    @DisplayName("[성공] 신규 Schedule을 저장하고 본인 스코프로 조회한다")
    void should_returnSchedule_when_savedAndFoundByIdAndUserId() {
        Schedule saved = schedule(7L, "회의", BASE);

        Optional<Schedule> found = scheduleRepository.findByIdAndUserId(saved.getScheduleId(), 7L);

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("회의");
        assertThat(found.get().getScheduleType()).isEqualTo(ScheduleType.PERSONAL);
        assertThat(found.get().getScheduledAt()).isEqualTo(BASE);
        assertThat(found.get().isDone()).isFalse();
    }

    @Test
    @DisplayName("[스펙] 반환된 Schedule은 전 필드가 온전하다 — id 채번 + auditing + turnId")
    void should_returnCompleteSchedule_when_found() {
        Schedule saved = scheduleRepository.save(
                Schedule.create(7L, 42L, "운동", "헬스장", ScheduleType.HEALTH, BASE));

        Schedule found = scheduleRepository.findByIdAndUserId(saved.getScheduleId(), 7L).orElseThrow();

        assertThat(found.getScheduleId()).isNotNull().isPositive();
        assertThat(found.getUserId()).isEqualTo(7L);
        assertThat(found.getTurnId()).isEqualTo(42L);
        assertThat(found.getContent()).isEqualTo("헬스장");
        assertThat(found.getDoneAt()).isNull();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("[경계] done()은 같은 행에 doneAt을 update한다")
    void should_updateDoneAt_when_doneScheduleSaved() {
        Schedule saved = schedule(7L, "회의", BASE);

        scheduleRepository.save(saved.done());

        Schedule found = scheduleRepository.findByIdAndUserId(saved.getScheduleId(), 7L).orElseThrow();
        assertThat(found.getScheduleId()).isEqualTo(saved.getScheduleId());   // 같은 행
        assertThat(found.isDone()).isTrue();
        assertThat(found.getDoneAt()).isNotNull();
    }

    @Test
    @DisplayName("[경계] 기간 조회는 [from, to) — from 포함, to 제외, scheduled_at 오름차순")
    void should_returnInPeriodOrdered_when_findAllByUserIdInPeriod() {
        schedule(7L, "경계 밖(이전)", BASE.minusSeconds(1));
        schedule(7L, "from 정확히", BASE);
        schedule(7L, "중간", BASE.plusSeconds(3600));
        schedule(7L, "to 정확히(제외)", BASE.plusSeconds(7200));

        List<Schedule> result = scheduleRepository.findAllByUserIdInPeriod(
                7L, BASE, BASE.plusSeconds(7200));

        assertThat(result).extracting(Schedule::getTitle)
                .containsExactly("from 정확히", "중간");
    }

    @Test
    @DisplayName("[경계] 다른 유저의 일정은 기간 조회에 섞이지 않는다")
    void should_isolateByUser_when_findAllByUserIdInPeriod() {
        schedule(7L, "내 일정", BASE);
        schedule(8L, "남의 일정", BASE);

        List<Schedule> result = scheduleRepository.findAllByUserIdInPeriod(
                7L, BASE, BASE.plusSeconds(1));

        assertThat(result).extracting(Schedule::getTitle).containsExactly("내 일정");
    }

    @Test
    @DisplayName("[실패] 타 유저 스코프로 단건 조회하면 empty — 소유권 차단")
    void should_returnEmpty_when_foundWithOtherUserId() {
        Schedule saved = schedule(7L, "내 일정", BASE);

        assertThat(scheduleRepository.findByIdAndUserId(saved.getScheduleId(), 8L)).isEmpty();
    }

    @Test
    @DisplayName("[실패/경계] soft-delete된 Schedule은 단건/기간 조회 모두에서 제외된다")
    void should_excludeSoftDeleted_when_find() {
        Schedule saved = schedule(7L, "삭제될 일정", BASE);
        ScheduleEntity entity = scheduleEntityRepository
                .findByIdAndUserIdAndIsDeletedFalse(saved.getScheduleId(), 7L).orElseThrow();
        scheduleEntityRepository.save(entity.softDelete());

        assertThat(scheduleRepository.findByIdAndUserId(saved.getScheduleId(), 7L)).isEmpty();
        assertThat(scheduleRepository.findAllByUserIdInPeriod(7L, BASE, BASE.plusSeconds(1))).isEmpty();
    }
}
