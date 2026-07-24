package com.chatbot.bravo.jdbc.auth.repository;

import com.chatbot.bravo.infrastructure.auth.repository.LoginSessionRepository;
import com.chatbot.bravo.model.auth.LoginSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DataJdbcTest
@ComponentScan(basePackages = "com.chatbot.bravo.jdbc.auth.repository")
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class LoginSessionJdbcRepositoryTest {

    @Autowired
    private LoginSessionRepository sessionRepository;             // 테스트 대상 (인터페이스)

    @Autowired
    private LoginSessionEntityRepository sessionEntityRepository; // soft-delete 픽스처용

    @Test
    @DisplayName("[성공] 신규 세션을 저장하고 sessionKey로 조회한다")
    void should_returnSession_when_savedAndFoundBySessionKey() {
        LoginSession saved = sessionRepository.save(LoginSession.issue(1L));

        Optional<LoginSession> found = sessionRepository.findBySessionKey(saved.getSessionKey());

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("[경계] 같은 세션 재저장(touch 경로)은 update — id 유지 + lastRequestedAt만 전진")
    void should_updateSameRow_when_touchedSessionSavedAgain() {
        LoginSession issued = sessionRepository.save(LoginSession.issue(1L));

        LoginSession touched = sessionRepository.save(issued.touch());

        assertThat(touched.getLoginSessionId()).isEqualTo(issued.getLoginSessionId());   // 같은 행
        assertThat(touched.getLastRequestedAt()).isAfterOrEqualTo(issued.getLastRequestedAt());
        assertThat(touched.getLastLoggedInAt())                                          // 로그인 시각 불변
                .isCloseTo(issued.getLastLoggedInAt(), within(1, ChronoUnit.MILLIS));
        assertThat(touched.getSessionKey()).isEqualTo(issued.getSessionKey());
    }

    @Test
    @DisplayName("[스펙] 반환된 LoginSession은 전 필드가 온전하다 (DATETIME(6) 왕복 + auditing)")
    void should_returnCompleteSession_when_foundBySessionKey() {
        LoginSession issued = LoginSession.issue(42L);
        sessionRepository.save(issued);

        LoginSession found = sessionRepository.findBySessionKey(issued.getSessionKey()).orElseThrow();

        assertThat(found.getLoginSessionId()).isNotNull().isPositive();
        assertThat(found.getSessionKey()).isEqualTo(issued.getSessionKey());
        assertThat(found.getUserId()).isEqualTo(42L);
        assertThat(found.getLastLoggedInAt()).isCloseTo(issued.getLastLoggedInAt(), within(1, ChronoUnit.MILLIS));
        assertThat(found.getLastRequestedAt()).isCloseTo(issued.getLastRequestedAt(), within(1, ChronoUnit.MILLIS));
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("[실패] 존재하지 않는 sessionKey면 empty를 반환한다")
    void should_returnEmpty_when_sessionKeyDoesNotExist() {
        assertThat(sessionRepository.findBySessionKey("no-such-key")).isEmpty();
    }

    @Test
    @DisplayName("[성공] deleteBySessionKey는 세션을 soft-delete해 조회에서 제외한다 (로그아웃 revoke)")
    void should_excludeSession_when_deletedBySessionKey() {
        LoginSession saved = sessionRepository.save(LoginSession.issue(1L));

        sessionRepository.deleteBySessionKey(saved.getSessionKey());

        assertThat(sessionRepository.findBySessionKey(saved.getSessionKey())).isEmpty();
    }

    @Test
    @DisplayName("[경계] deleteBySessionKey는 멱등 — 없는/이미 삭제된 키여도 예외 없이 통과")
    void should_notThrow_when_deletingNonExistingSessionKey() {
        // 없는 키
        sessionRepository.deleteBySessionKey("no-such-key");

        // 이미 삭제된 키 재삭제
        LoginSession saved = sessionRepository.save(LoginSession.issue(1L));
        sessionRepository.deleteBySessionKey(saved.getSessionKey());
        sessionRepository.deleteBySessionKey(saved.getSessionKey());

        assertThat(sessionRepository.findBySessionKey(saved.getSessionKey())).isEmpty();
    }

    @Test
    @DisplayName("[실패/경계] soft-delete된 세션은 findBySessionKey에서 제외된다")
    void should_returnEmpty_when_sessionIsSoftDeleted() {
        LoginSession saved = sessionRepository.save(LoginSession.issue(1L));
        LoginSessionEntity entity = sessionEntityRepository
                .findBySessionKeyAndIsDeletedFalse(saved.getSessionKey()).orElseThrow();
        sessionEntityRepository.save(entity.softDelete());

        assertThat(sessionRepository.findBySessionKey(saved.getSessionKey())).isEmpty();
    }
}
