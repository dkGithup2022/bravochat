package com.chatbot.bravo.service.auth;

import com.chatbot.bravo.exception.auth.LoginFailedException;
import com.chatbot.bravo.infrastructure.auth.repository.UserRepository;
import com.chatbot.bravo.model.auth.LoginSession;
import com.chatbot.bravo.model.auth.User;
import com.chatbot.bravo.service.auth.dto.LoginCommand;
import com.chatbot.bravo.service.auth.dto.LoginResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginUsecaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordVerifier passwordVerifier;

    @Mock
    private SessionManager sessionManager;

    @InjectMocks
    private LoginUsecase loginUsecase;

    private static final Instant NOW = Instant.now();

    private User user() {
        return new User(1L, "tester", "hashed-password", NOW, NOW);
    }

    @Test
    @DisplayName("로그인 성공 시 발급된 세션의 sessionKey를 반환한다")
    void should_returnSessionKey_when_loginSucceeds() {
        LoginSession session = LoginSession.issue(1L);
        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(user()));
        when(passwordVerifier.matches("raw", "hashed-password")).thenReturn(true);
        when(sessionManager.newOne(1L)).thenReturn(session);

        LoginResult result = loginUsecase.login(new LoginCommand("tester", "raw"));

        assertThat(result.sessionKey()).isEqualTo(session.getSessionKey());
    }

    @Test
    @DisplayName("존재하지 않는 username이면 LoginFailedException(401) — 세션 발급 없음")
    void should_throwLoginFailed_when_userNotFound() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginUsecase.login(new LoginCommand("nobody", "raw")))
                .isInstanceOf(LoginFailedException.class);

        verify(sessionManager, never()).newOne(anyLong());
    }

    @Test
    @DisplayName("비밀번호 불일치면 LoginFailedException(401) — 세션 발급 없음")
    void should_throwLoginFailed_when_passwordMismatch() {
        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(user()));
        when(passwordVerifier.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> loginUsecase.login(new LoginCommand("tester", "wrong")))
                .isInstanceOf(LoginFailedException.class);

        verify(sessionManager, never()).newOne(anyLong());
    }

    @Test
    @DisplayName("계정 없음과 비번 불일치의 클라이언트 메시지·상태코드는 동일하다 (존재 여부 누수 방지)")
    void should_exposeIdenticalMessage_when_eitherFailure() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(user()));
        when(passwordVerifier.matches(anyString(), anyString())).thenReturn(false);

        LoginFailedException notFound = catchLoginFailure("nobody");
        LoginFailedException mismatch = catchLoginFailure("tester");

        assertThat(notFound.httpErrorMessage()).isEqualTo(mismatch.httpErrorMessage());
        assertThat(notFound.httpStatusCode()).isEqualTo(401);
    }

    private LoginFailedException catchLoginFailure(String username) {
        try {
            loginUsecase.login(new LoginCommand(username, "raw"));
        } catch (LoginFailedException e) {
            return e;
        }
        throw new AssertionError("LoginFailedException이 발생해야 한다");
    }
}
