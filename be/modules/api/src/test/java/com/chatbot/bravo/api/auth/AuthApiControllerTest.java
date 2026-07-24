package com.chatbot.bravo.api.auth;

import com.chatbot.bravo.api.auth.dto.LoginRequest;
import com.chatbot.bravo.model.auth.LoginSession;
import com.chatbot.bravo.service.auth.LoginUsecase;
import com.chatbot.bravo.service.auth.LogoutUsecase;
import com.chatbot.bravo.service.auth.dto.LoginCommand;
import com.chatbot.bravo.service.auth.dto.LoginResult;
import com.chatbot.bravo.service.auth.dto.LogoutCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * AuthApiController 단위 테스트 — MockMvc 없이 메서드 직접 호출.
 * 컨트롤러는 얇으므로 "요청→command 배선"과 "세션키 응답 헤더/위임"만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class AuthApiControllerTest {

    @Mock
    private LoginUsecase loginUsecase;

    @Mock
    private LogoutUsecase logoutUsecase;

    @InjectMocks
    private AuthApiController controller;

    private LoginSession session(String sessionKey) {
        Instant now = Instant.now();
        return new LoginSession(1L, sessionKey, 7L, now, now, now, now);
    }

    @Test
    @DisplayName("login: 요청을 command로 넘기고, 발급된 세션키를 Authorization 헤더로 204 반환한다")
    void should_returnSessionKeyHeader_when_login() {
        when(loginUsecase.login(any(LoginCommand.class))).thenReturn(new LoginResult("key-abc"));

        ResponseEntity<Void> response = controller.login(new LoginRequest("tester", "pw1234"));

        // 응답: 204 + Authorization: Bearer key-abc
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer key-abc");

        // 배선: 요청 필드가 command로 그대로 전달
        ArgumentCaptor<LoginCommand> captor = ArgumentCaptor.forClass(LoginCommand.class);
        org.mockito.Mockito.verify(loginUsecase).login(captor.capture());
        assertThat(captor.getValue().username()).isEqualTo("tester");
        assertThat(captor.getValue().rawPassword()).isEqualTo("pw1234");
    }

    @Test
    @DisplayName("logout: 인증 세션의 sessionKey로 LogoutCommand를 만들어 위임하고 204 반환한다")
    void should_delegateWithSessionKey_when_logout() {
        ResponseEntity<Void> response = controller.logout(session("sk-9"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ArgumentCaptor<LogoutCommand> captor = ArgumentCaptor.forClass(LogoutCommand.class);
        org.mockito.Mockito.verify(logoutUsecase).logout(captor.capture());
        assertThat(captor.getValue().sessionKey()).isEqualTo("sk-9");
    }
}
