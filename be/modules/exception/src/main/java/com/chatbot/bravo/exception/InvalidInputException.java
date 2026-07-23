package com.chatbot.bravo.exception;

public class InvalidInputException extends DomainException implements HttpException {

    private final int statusCode;
    private final String clientMessage;

    public InvalidInputException(String detail) {
        this(detail, 400, "잘못된 요청입니다");
    }

    public InvalidInputException(String detail, int statusCode, String clientMessage) {
        super(detail);
        this.statusCode = statusCode;
        this.clientMessage = clientMessage;
    }

    @Override public int httpStatusCode() { return statusCode; }
    @Override public String httpErrorMessage() { return clientMessage; }
}
