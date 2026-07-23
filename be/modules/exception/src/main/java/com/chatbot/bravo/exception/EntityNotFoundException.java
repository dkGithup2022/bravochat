package com.chatbot.bravo.exception;

public abstract class EntityNotFoundException extends DomainException implements HttpException {

    private final int statusCode;
    private final String clientMessage;

    protected EntityNotFoundException(String detail) {
        this(detail, 404, "해당 리소스를 찾을 수 없습니다");
    }

    protected EntityNotFoundException(String detail, int statusCode, String clientMessage) {
        super(detail);
        this.statusCode = statusCode;
        this.clientMessage = clientMessage;
    }

    @Override public int httpStatusCode() { return statusCode; }
    @Override public String httpErrorMessage() { return clientMessage; }
}
