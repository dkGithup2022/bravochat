package com.chatbot.bravo.service.auth;

import com.chatbot.bravo.exception.auth.LoginFailedException;
import com.chatbot.bravo.infrastructure.auth.repository.UserRepository;
import com.chatbot.bravo.model.auth.LoginSession;
import com.chatbot.bravo.model.auth.User;
import com.chatbot.bravo.service.auth.dto.LoginCommand;
import com.chatbot.bravo.service.auth.dto.LoginResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginUsecase {

    private final UserRepository userRepository;
    private final PasswordVerifier passwordVerifier;
    private final SessionManager sessionManager;

    /**
     * 로그인: 유저 조회 → 비밀번호 검증 → 세션 발급.
     * 계정 없음/비번 불일치는 동일한 LoginFailedException(401) — 상세 사유는 detail에만.
     */
    public LoginResult login(LoginCommand command) {
        User user = userRepository.findByUsername(command.username())
                .orElseThrow(() -> new LoginFailedException(
                        "user not found: username=" + command.username()));

        if (!passwordVerifier.matches(command.rawPassword(), user.getPassword())) {
            throw new LoginFailedException("password mismatch: userId=" + user.getUserId());
        }

        LoginSession session = sessionManager.newOne(user.getUserId());
        log.info("login success: userId={}", user.getUserId());
        return new LoginResult(session.getSessionKey());
    }
}
