package com.chatbot.bravo.api.auth;

import com.chatbot.bravo.exception.auth.InvalidSessionException;
import com.chatbot.bravo.model.auth.LoginSession;
import com.chatbot.bravo.service.auth.SessionManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.NativeWebRequest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * LoginUserArgumentResolver 단위 테스트 — api 계층의 인증 게이트.
 * Authorization 헤더 파싱 분기(Bearer/누락/형식오류/빈키) × required 여부 × 세션 검증 결과를 검증한다.
 * MVC 컨텍스트 없이 실제 MethodParameter + NativeWebRequest(mock)로 직접 호출한다.
 */
@ExtendWith(MockitoExtension.class)
class LoginUserArgumentResolverTest {

    @Mock
    private SessionManager sessionManager;

    @Mock
    private NativeWebRequest webRequest;

    /** @LoginUser 파라미터 시그니처 샘플 — 실제 MethodParameter를 얻기 위한 대상. */
    @SuppressWarnings("unused")
    static class Sample {
        void required(@LoginUser LoginSession s) {}
        void optional(@LoginUser(required = false) LoginSession s) {}
        void notAnnotated(LoginSession s) {}
        void wrongType(@LoginUser String s) {}
    }

    private MethodParameter param(String method, Class<?> type) throws NoSuchMethodException {
        return new MethodParameter(Sample.class.getDeclaredMethod(method, type), 0);
    }

    private LoginSession session(Long userId, String key) {
        Instant now = Instant.now();
        return new LoginSession(1L, key, userId, now, now, now, now);
    }

    private LoginUserArgumentResolver resolver() {
        return new LoginUserArgumentResolver(sessionManager);
    }

    // ---------------- supportsParameter ----------------

    @Test
    @DisplayName("@LoginUser + LoginSession 타입 파라미터면 지원한다")
    void should_support_when_annotatedLoginSession() throws Exception {
        assertThat(resolver().supportsParameter(param("required", LoginSession.class))).isTrue();
    }

    @Test
    @DisplayName("애노테이션 없으면 지원하지 않는다")
    void should_notSupport_when_notAnnotated() throws Exception {
        assertThat(resolver().supportsParameter(param("notAnnotated", LoginSession.class))).isFalse();
    }

    @Test
    @DisplayName("@LoginUser라도 타입이 LoginSession이 아니면 지원하지 않는다")
    void should_notSupport_when_wrongType() throws Exception {
        assertThat(resolver().supportsParameter(param("wrongType", String.class))).isFalse();
    }

    // ---------------- resolveArgument: 정상 ----------------

    @Test
    @DisplayName("유효한 Bearer 토큰이면 SessionManager.check로 검증한 세션을 주입한다")
    void should_injectSession_when_validBearer() throws Exception {
        LoginSession session = session(7L, "key-123");
        when(webRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer key-123");
        when(sessionManager.check("key-123")).thenReturn(session);

        Object result = resolver().resolveArgument(param("required", LoginSession.class), null, webRequest, null);

        assertThat(result).isSameAs(session);
        verify(sessionManager).check("key-123");
    }

    // ---------------- resolveArgument: required=true 실패 분기 ----------------

    @Test
    @DisplayName("[required] 헤더가 없으면 InvalidSessionException — 검증 시도 안 함")
    void should_throw_when_headerMissingAndRequired() throws Exception {
        when(webRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        assertThatThrownBy(() ->
                resolver().resolveArgument(param("required", LoginSession.class), null, webRequest, null))
                .isInstanceOf(InvalidSessionException.class);

        verify(sessionManager, never()).check(anyString());
    }

    @Test
    @DisplayName("[required] Bearer 접두어가 없으면 없는 것으로 간주 → InvalidSessionException")
    void should_throw_when_notBearerPrefix() throws Exception {
        when(webRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Token abc");

        assertThatThrownBy(() ->
                resolver().resolveArgument(param("required", LoginSession.class), null, webRequest, null))
                .isInstanceOf(InvalidSessionException.class);

        verify(sessionManager, never()).check(anyString());
    }

    @Test
    @DisplayName("[required] Bearer 뒤 키가 비어있으면 InvalidSessionException")
    void should_throw_when_emptyKey() throws Exception {
        when(webRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer    ");

        assertThatThrownBy(() ->
                resolver().resolveArgument(param("required", LoginSession.class), null, webRequest, null))
                .isInstanceOf(InvalidSessionException.class);

        verify(sessionManager, never()).check(anyString());
    }

    @Test
    @DisplayName("[required] 세션 검증이 실패(InvalidSessionException)하면 그대로 전파한다")
    void should_propagate_when_checkFailsAndRequired() throws Exception {
        when(webRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer expired");
        when(sessionManager.check("expired")).thenThrow(new InvalidSessionException("expired"));

        assertThatThrownBy(() ->
                resolver().resolveArgument(param("required", LoginSession.class), null, webRequest, null))
                .isInstanceOf(InvalidSessionException.class);
    }

    // ---------------- resolveArgument: required=false → null ----------------

    @Test
    @DisplayName("[optional] 헤더가 없으면 null을 주입한다 — 검증 시도 안 함")
    void should_returnNull_when_headerMissingAndOptional() throws Exception {
        when(webRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        Object result = resolver().resolveArgument(param("optional", LoginSession.class), null, webRequest, null);

        assertThat(result).isNull();
        verify(sessionManager, never()).check(anyString());
    }

    @Test
    @DisplayName("[optional] 세션 검증이 실패하면 예외 대신 null을 주입한다")
    void should_returnNull_when_checkFailsAndOptional() throws Exception {
        when(webRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer expired");
        when(sessionManager.check("expired")).thenThrow(new InvalidSessionException("expired"));

        Object result = resolver().resolveArgument(param("optional", LoginSession.class), null, webRequest, null);

        assertThat(result).isNull();
    }
}
