package com.chatbot.bravo.service.auth.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/** mock 없는 순수 단위 — BCrypt 위임 검증. */
class BcryptPasswordVerifierTest {

    private final BcryptPasswordVerifier verifier = new BcryptPasswordVerifier();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("[성공] 저장된 해시와 일치하는 평문이면 true")
    void should_returnTrue_when_rawPasswordMatchesStoredHash() {
        String storedHash = encoder.encode("password1234");

        assertThat(verifier.matches("password1234", storedHash)).isTrue();
    }

    @Test
    @DisplayName("[실패] 일치하지 않는 평문이면 false")
    void should_returnFalse_when_rawPasswordDoesNotMatch() {
        String storedHash = encoder.encode("password1234");

        assertThat(verifier.matches("wrong-password", storedHash)).isFalse();
    }

    @Test
    @DisplayName("[경계] salt 때문에 해시는 매번 다르지만 매칭은 항상 성립한다")
    void should_matchConsistently_when_hashesDifferBySalt() {
        String hash1 = encoder.encode("password1234");
        String hash2 = encoder.encode("password1234");

        assertThat(hash1).isNotEqualTo(hash2);                        // 해시 자체는 다름
        assertThat(verifier.matches("password1234", hash1)).isTrue();
        assertThat(verifier.matches("password1234", hash2)).isTrue();
    }
}
