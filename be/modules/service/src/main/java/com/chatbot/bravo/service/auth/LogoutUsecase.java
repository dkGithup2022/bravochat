package com.chatbot.bravo.service.auth;

import com.chatbot.bravo.service.auth.dto.LogoutCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogoutUsecase {

    private final SessionManager sessionManager;

    /**
     * 로그아웃: sessionKey로 세션을 만료(soft-delete)한다.
     * 인증 계층 도입 전이므로 소유권 검사는 하지 않는다 — sessionKey만으로 revoke. 멱등.
     */
    public void logout(LogoutCommand command) {
        sessionManager.revoke(command.sessionKey());
    }
}
