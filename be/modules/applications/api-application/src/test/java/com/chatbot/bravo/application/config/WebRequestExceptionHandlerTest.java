package com.chatbot.bravo.application.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * WebRequestExceptionHandler 단위 테스트 — Spring MVC 바인딩 실패를 400으로 매핑하는 로직.
 * 핸들러 메서드를 직접 호출한다.
 */
class WebRequestExceptionHandlerTest {

    private final WebRequestExceptionHandler handler = new WebRequestExceptionHandler();

    @Test
    @DisplayName("필수 파라미터 누락 → 400 MissingParameter, 파라미터명을 메시지에 포함한다")
    void should_map400_when_missingParam() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("size", "int");

        ResponseEntity<ErrorResponse> response = handler.handleMissingParam(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("MissingParameter");
        assertThat(response.getBody().message()).contains("size");
    }

    @Test
    @DisplayName("파라미터 타입 불일치 → 400 TypeMismatch, 파라미터명을 메시지에 포함한다")
    void should_map400_when_typeMismatch() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("size");

        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("TypeMismatch");
        assertThat(response.getBody().message()).contains("size");
    }

    @Test
    @DisplayName("읽을 수 없는(깨진) 요청 바디 → 400 MalformedBody")
    void should_map400_when_bodyNotReadable() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);

        ResponseEntity<ErrorResponse> response = handler.handleNotReadable(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("MalformedBody");
    }
}
