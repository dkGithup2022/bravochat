package com.chatbot.bravo.exception;

public class ConflictException extends DomainException implements HttpException {

    private final int statusCode;
    private final String clientMessage;

    public ConflictException(String detail) {
        this(detail, 409, "이미 존재하는 리소스입니다");
    }

    public ConflictException(String detail, int statusCode, String clientMessage) {
        super(detail);
        this.statusCode = statusCode;
        this.clientMessage = clientMessage;
    }

    @Override public int httpStatusCode() { return statusCode; }
    @Override public String httpErrorMessage() { return clientMessage; }
}
