package com.chatbot.bravo.exception;

public abstract class DomainException extends RuntimeException {

    private final String detail;

    protected DomainException(String detail) {
        super(detail);
        this.detail = detail;
    }

    public String getDetail() {
        return detail;
    }
}
