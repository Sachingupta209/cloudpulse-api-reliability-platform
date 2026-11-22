package com.cloudpulse.exception;

public class RateLimitExceededException extends ApiException {
    public RateLimitExceededException(String message) {
        super(message);
    }
}
