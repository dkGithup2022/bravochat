package com.chatbot.bravo.service.auth.impl;

import com.chatbot.bravo.exception.auth.InvalidSessionException;
import com.chatbot.bravo.infrastructure.auth.repository.LoginSessionRepository;
import com.chatbot.bravo.model.auth.LoginSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultSessionManagerTest {

    @Mock
    private LoginSessionRepository sessionRepository;

    @InjectMocks
    private DefaultSessionManager sessionManager;

    @Test
    @DisplayName("newOne은 새 세션을 발급해 저장한다")
    void should_issueAndSaveSession_when_newOne() {
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LoginSession session = sessionManager.newOne(1L);

        assertThat(session.getUserId()).isEqualTo(1L);
        assertThat(session.getSessionKey()).isNotBlank();
        verify(sessionRepository).save(any(LoginSession.class));
    }

    @Test
    @DisplayName("check: 유효 세션이면 lastRequestedAt을 touch하여 저장 후 반환한다")
    void should_touchAndReturnSession_when_checkValidSession() {
        LoginSession session = LoginSession.issue(1L);
        when(sessionRepository.findBySessionKey(session.getSessionKey())).thenReturn(Optional.of(session));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LoginSession checked = sessionManager.check(session.getSessionKey());

        assertThat(checked.getUserId()).isEqualTo(1L);
        assertThat(checked.getLastRequestedAt()).isAfterOrEqualTo(session.getLastRequestedAt());
        verify(sessionRepository).save(any(LoginSession.class));
    }

    @Test
    @DisplayName("check: 세션이 없으면 InvalidSessionException(401)")
    void should_throwInvalidSession_when_sessionKeyNotFound() {
        when(sessionRepository.findBySessionKey("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionManager.check("unknown"))
                .isInstanceOf(InvalidSessionException.class);
    }

    @Test
    @DisplayName("check: 로그인 7일 경과(절대만료) 세션이면 InvalidSessionException — touch 저장 없음")
    void should_throwInvalidSession_when_sessionExpired() {
        Instant eightDaysAgo = Instant.now().minus(Duration.ofDays(8));   // TTL 7일 초과
        LoginSession expired = new LoginSession(
                1L, "expired-key", 1L, eightDaysAgo, eightDaysAgo, eightDaysAgo, eightDaysAgo);
        when(sessionRepository.findBySessionKey("expired-key")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> sessionManager.check("expired-key"))
                .isInstanceOf(InvalidSessionException.class);

        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("revoke는 sessionKey로 세션을 만료(soft-delete)한다")
    void should_delegateToRepository_when_revoke() {
        sessionManager.revoke("some-key");

        verify(sessionRepository).deleteBySessionKey("some-key");
    }
}
