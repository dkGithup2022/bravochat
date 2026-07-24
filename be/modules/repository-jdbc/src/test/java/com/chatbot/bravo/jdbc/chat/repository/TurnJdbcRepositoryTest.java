package com.chatbot.bravo.jdbc.chat.repository;

import com.chatbot.bravo.infrastructure.chat.repository.TurnRepository;
import com.chatbot.bravo.model.chat.Turn;
import com.chatbot.bravo.model.chat.TurnStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJdbcTest
@ComponentScan(basePackages = "com.chatbot.bravo.jdbc.chat.repository")
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class TurnJdbcRepositoryTest {

    @Autowired
    private TurnRepository turnRepository;          // 테스트 대상 (인터페이스)

    @Autowired
    private TurnEntityRepository turnEntityRepository;  // soft-delete 픽스처용

    @Test
    @DisplayName("[성공] 신규 Turn을 저장하고 id로 조회한다 (PROCESSING)")
    void should_returnTurn_when_savedAndFoundById() {
        Turn saved = turnRepository.save(Turn.start(7L));

        Optional<Turn> found = turnRepository.findById(saved.getTurnId());

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(7L);
        assertThat(found.get().getStatus()).isEqualTo(TurnStatus.PROCESSING);
    }

    @Test
    @DisplayName("[스펙] 반환된 Turn은 전 필드가 온전하다 — id 채번 + auditing")
    void should_returnCompleteTurn_when_foundById() {
        Turn saved = turnRepository.save(Turn.start(7L));

        Turn turn = turnRepository.findById(saved.getTurnId()).orElseThrow();

        assertThat(turn.getTurnId()).isNotNull().isPositive();
        assertThat(turn.getUserId()).isEqualTo(7L);
        assertThat(turn.getStatus()).isEqualTo(TurnStatus.PROCESSING);
        assertThat(turn.getCompletedAt()).isNull();      // 아직 미완료
        assertThat(turn.getFailureReason()).isNull();
        assertThat(turn.getCreatedAt()).isNotNull();
        assertThat(turn.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("[경계] complete()는 같은 행을 COMPLETED로 update + completedAt 세팅")
    void should_updateToCompleted_when_completedTurnSaved() {
        Turn started = turnRepository.save(Turn.start(7L));

        turnRepository.save(started.complete());

        Turn found = turnRepository.findById(started.getTurnId()).orElseThrow();
        assertThat(found.getTurnId()).isEqualTo(started.getTurnId());   // 같은 행
        assertThat(found.getStatus()).isEqualTo(TurnStatus.COMPLETED);
        assertThat(found.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("[경계] fail()은 FAILED로 update + failureReason 저장")
    void should_updateToFailed_when_failedTurnSaved() {
        Turn started = turnRepository.save(Turn.start(7L));

        turnRepository.save(started.fail("llm timeout"));

        Turn found = turnRepository.findById(started.getTurnId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(TurnStatus.FAILED);
        assertThat(found.getFailureReason()).isEqualTo("llm timeout");
        assertThat(found.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("[실패] 존재하지 않는 id면 empty를 반환한다")
    void should_returnEmpty_when_idDoesNotExist() {
        assertThat(turnRepository.findById(999_999L)).isEmpty();
    }

    @Test
    @DisplayName("[실패/경계] soft-delete된 Turn은 조회에서 제외된다")
    void should_returnEmpty_when_turnIsSoftDeleted() {
        Turn saved = turnRepository.save(Turn.start(7L));
        TurnEntity entity = turnEntityRepository.findByIdAndIsDeletedFalse(saved.getTurnId()).orElseThrow();
        turnEntityRepository.save(entity.softDelete());

        assertThat(turnRepository.findById(saved.getTurnId())).isEmpty();
    }
}
