package com.chatbot.bravo.exception;

public class ForbiddenException extends DomainException implements HttpException {

    private final int statusCode;
    private final String clientMessage;

    public ForbiddenException(String detail) {
        this(detail, 403, "접근 권한이 없습니다");
    }

    public ForbiddenException(String detail, int statusCode, String clientMessage) {
        super(detail);
        this.statusCode = statusCode;
        this.clientMessage = clientMessage;
    }

    @Override public int httpStatusCode() { return statusCode; }
    @Override public String httpErrorMessage() { return clientMessage; }
}
