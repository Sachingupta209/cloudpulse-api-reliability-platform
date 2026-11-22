package com.cloudpulse.exception;

import java.time.Instant;
import java.util.Map;

public class ErrorResponse {
    private final boolean success;
    private final int status;
    private final String error;
    private final String message;
    private final Instant timestamp;
    private final Map<String, String> validationErrors;

    public ErrorResponse(int status, String error, String message, Map<String, String> validationErrors) {
        this.success = false;
        this.status = status;
        this.error = error;
        this.message = message;
        this.timestamp = Instant.now();
        this.validationErrors = validationErrors;
    }

    public boolean isSuccess() {
        return success;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Map<String, String> getValidationErrors() {
        return validationErrors;
    }
}
