package com.chatbot.bravo.exception;

public interface HttpException {

    int httpStatusCode();

    String httpErrorMessage();
}
