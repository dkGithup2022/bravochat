package com.chatbot.bravo.application.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Spring MVC 요청 바인딩 실패(필수 파라미터 누락, 타입 불일치, 잘못된 바디)를 400으로 매핑한다.
 * be-init 소유의 GlobalExceptionHandler(Exception → 500)보다 먼저 처리되도록 @Order 우선순위를 높인다. (C-7)
 */
@Slf4j
@Order(0)
@RestControllerAdvice
public class WebRequestExceptionHandler {

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> handleMissingParam(
            MissingServletRequestParameterException e) {
        log.info("Missing request parameter: {}", e.getParameterName());
        return badRequest("MissingParameter",
                "필수 파라미터가 누락되었습니다: " + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException e) {
        log.info("Type mismatch for parameter: {}", e.getName());
        return badRequest("TypeMismatch",
                "파라미터 형식이 올바르지 않습니다: " + e.getName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> handleNotReadable(
            HttpMessageNotReadableException e) {
        log.info("Malformed request body: {}", e.getMessage());
        return badRequest("MalformedBody", "요청 본문을 읽을 수 없습니다");
    }

    private org.springframework.http.ResponseEntity<ErrorResponse> badRequest(String error, String message) {
        return org.springframework.http.ResponseEntity.badRequest()
                .body(new ErrorResponse(400, error, message));
    }
}
