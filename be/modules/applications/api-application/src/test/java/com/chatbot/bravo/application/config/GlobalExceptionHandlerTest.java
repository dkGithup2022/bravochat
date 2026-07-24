package com.chatbot.bravo.application.config;

import com.chatbot.bravo.exception.DomainException;
import com.chatbot.bravo.exception.auth.InvalidSessionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * GlobalExceptionHandler 단위 테스트 — 예외를 HTTP status/body(ErrorResponse)로 매핑하는 순수 로직.
 * 핸들러 메서드를 직접 호출한다 (MVC 컨텍스트 불필요).
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    /** HttpException을 구현하지 않은 도메인 예외 — 500 분기 검증용. */
    static class PlainDomainException extends DomainException {
        PlainDomainException(String detail) {
            super(detail);
        }
    }

    @Test
    @DisplayName("HttpException 도메인 예외는 그 status/message를 그대로 ErrorResponse로 매핑한다")
    void should_mapToHttpStatus_when_httpException() {
        InvalidSessionException ex = new InvalidSessionException("no session");

        ResponseEntity<ErrorResponse> response = handler.handleDomain(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(ex.httpStatusCode());   // 401
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(ex.httpStatusCode());
        assertThat(body.error()).isEqualTo("InvalidSessionException");
        assertThat(body.message()).isEqualTo(ex.httpErrorMessage());
    }

    @Test
    @DisplayName("HttpException이 아닌 도메인 예외는 500 Internal Server Error로 매핑한다")
    void should_map500_when_notHttpException() {
        ResponseEntity<ErrorResponse> response = handler.handleDomain(new PlainDomainException("boom"));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().message()).isEqualTo("Internal server error");
    }

    @Test
    @DisplayName("바디 검증 실패는 필드별 메시지를 모아 400 ValidationFailed로 매핑한다")
    void should_map400_when_validationFails() {
        BindingResult binding = new BeanPropertyBindingResult(new Object(), "req");
        binding.addError(new FieldError("req", "message", "must not be blank"));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(binding);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("ValidationFailed");
        assertThat(response.getBody().message()).contains("message").contains("must not be blank");
    }
}
