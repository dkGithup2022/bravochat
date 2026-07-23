package com.chatbot.bravo.api.auth;

import com.chatbot.bravo.api.auth.dto.LoginRequest;
import com.chatbot.bravo.exception.auth.InvalidSessionException;
import com.chatbot.bravo.service.auth.LoginUsecase;
import com.chatbot.bravo.service.auth.LogoutUsecase;
import com.chatbot.bravo.service.auth.dto.LoginResult;
import com.chatbot.bravo.service.auth.dto.LogoutCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Auth", description = "인증 API")
public class AuthApiController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final LoginUsecase loginUsecase;
    private final LogoutUsecase logoutUsecase;

    @Operation(summary = "로그인 — 성공 시 Authorization 응답 헤더로 세션 키 발급 (인증 불필요한 유일한 엔드포인트)")
    @PostMapping("/auth/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /auth/login - username={}", request.getUsername());
        LoginResult result = loginUsecase.login(request.toCommand());
        return ResponseEntity.noContent()
                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + result.sessionKey())
                .build();
    }

    @Operation(summary = "로그아웃 — Authorization 헤더의 세션 키를 만료시킨다")
    @DeleteMapping("/auth/session")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        String sessionKey = extractSessionKey(authorization);
        log.info("DELETE /auth/session");
        logoutUsecase.logout(new LogoutCommand(sessionKey));
        return ResponseEntity.noContent().build();
    }

    private String extractSessionKey(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new InvalidSessionException("missing or malformed Authorization header");
        }
        String sessionKey = authorization.substring(BEARER_PREFIX.length()).trim();
        if (sessionKey.isEmpty()) {
            throw new InvalidSessionException("empty session key");
        }
        return sessionKey;
    }
}
