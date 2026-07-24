package com.chatbot.bravo.service.auth;

import com.chatbot.bravo.service.auth.dto.LogoutCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LogoutUsecaseTest {

    @Mock
    private SessionManager sessionManager;

    @InjectMocks
    private LogoutUsecase logoutUsecase;

    @Test
    @DisplayName("logout은 sessionKey로 세션을 revoke한다 (소유권 검사 없음 — 인증 계층 도입 전)")
    void should_revokeSession_when_logout() {
        logoutUsecase.logout(new LogoutCommand("session-key"));

        verify(sessionManager).revoke("session-key");
    }
}
