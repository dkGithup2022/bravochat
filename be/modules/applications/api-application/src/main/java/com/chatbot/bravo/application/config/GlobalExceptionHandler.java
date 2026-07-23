package com.chatbot.bravo.application.config;

import com.chatbot.bravo.exception.DomainException;
import com.chatbot.bravo.exception.HttpException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        log.info("Validation failed: {}", e.getMessage());
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .collect(java.util.stream.Collectors.joining(", "));
        return ResponseEntity.badRequest().body(new ErrorResponse(400, "ValidationFailed", message));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomain(DomainException e) {
        if (e instanceof HttpException httpEx) {
            int status = httpEx.httpStatusCode();
            log.warn("Domain error [{}] - {}: {}", status, e.getClass().getSimpleName(), e.getMessage());
            return ResponseEntity.status(status).body(
                new ErrorResponse(status, e.getClass().getSimpleName(), httpEx.httpErrorMessage())
            );
        }
        log.error("Unhandled domain error - {}: {}", e.getClass().getSimpleName(), e.getMessage(), e);
        return ResponseEntity.internalServerError().body(
            new ErrorResponse(500, e.getClass().getSimpleName(), "Internal server error")
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return ResponseEntity.internalServerError().body(
            new ErrorResponse(500, "InternalServerError", "Internal server error")
        );
    }
}
