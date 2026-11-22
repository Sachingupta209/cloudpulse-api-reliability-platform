package com.cloudpulse.analytics;

public class ResponseTimeTrendDto {

    private final String timestamp;
    private final double avgResponseTimeMs;
    private final long totalChecks;
    private final long successfulChecks;

    public ResponseTimeTrendDto(String timestamp, double avgResponseTimeMs, long totalChecks, long successfulChecks) {
        this.timestamp = timestamp;
        this.avgResponseTimeMs = avgResponseTimeMs;
        this.totalChecks = totalChecks;
        this.successfulChecks = successfulChecks;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public double getAvgResponseTimeMs() {
        return avgResponseTimeMs;
    }

    public long getTotalChecks() {
        return totalChecks;
    }

    public long getSuccessfulChecks() {
        return successfulChecks;
    }
}
