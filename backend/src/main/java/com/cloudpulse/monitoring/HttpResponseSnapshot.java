package com.cloudpulse.monitoring;

import java.util.List;
import java.util.Map;

public class HttpResponseSnapshot {

    private final boolean successfulConnection;
    private final int statusCode;
    private final long responseTimeMs;
    private final Map<String, List<String>> headers;
    private final String body;
    private final String ipAddress;
    private final String errorMessage;

    public HttpResponseSnapshot(boolean successfulConnection, int statusCode, long responseTimeMs,
                                Map<String, List<String>> headers, String body, String ipAddress, String errorMessage) {
        this.successfulConnection = successfulConnection;
        this.statusCode = statusCode;
        this.responseTimeMs = responseTimeMs;
        this.headers = headers;
        this.body = body;
        this.ipAddress = ipAddress;
        this.errorMessage = errorMessage;
    }

    public static HttpResponseSnapshot failure(String errorMessage, long responseTimeMs, String ipAddress) {
        return new HttpResponseSnapshot(false, 0, responseTimeMs, Map.of(), null, ipAddress, errorMessage);
    }

    public boolean isSuccessfulConnection() {
        return successfulConnection;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
