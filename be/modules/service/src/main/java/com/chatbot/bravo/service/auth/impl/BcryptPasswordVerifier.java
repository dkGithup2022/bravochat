package com.chatbot.bravo.service.auth.impl;

import com.chatbot.bravo.service.auth.PasswordVerifier;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * BCrypt 기반 구현. spring-security-crypto 단일 모듈만 사용 (Security 필터체인 없음).
 */
@Component
class BcryptPasswordVerifier implements PasswordVerifier {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public boolean matches(String rawPassword, String storedPassword) {
        return encoder.matches(rawPassword, storedPassword);
    }
}
