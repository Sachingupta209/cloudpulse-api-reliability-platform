package com.cloudpulse.exception;

public class SsrfViolationException extends ApiException {
    public SsrfViolationException(String message) {
        super(message);
    }
}
