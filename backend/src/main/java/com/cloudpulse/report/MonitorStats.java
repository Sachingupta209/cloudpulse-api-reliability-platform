package com.cloudpulse.report;

import com.cloudpulse.monitor.Monitor;

public class MonitorStats {

    private final Monitor monitor;
    private final long totalChecks;
    private final long successChecks;
    private final long failureChecks;
    private final Double uptimePercentage;
    private final Double failureRate;
    private final Double avgLatencyMs;
    private final Long minLatencyMs;
    private final Long maxLatencyMs;
    private final Long p95LatencyMs;
    private final Long p99LatencyMs;

    public MonitorStats(Monitor monitor, long totalChecks, long successChecks, long failureChecks,
                        Double uptimePercentage, Double failureRate, Double avgLatencyMs,
                        Long minLatencyMs, Long maxLatencyMs, Long p95LatencyMs, Long p99LatencyMs) {
        this.monitor = monitor;
        this.totalChecks = totalChecks;
        this.successChecks = successChecks;
        this.failureChecks = failureChecks;
        this.uptimePercentage = uptimePercentage;
        this.failureRate = failureRate;
        this.avgLatencyMs = avgLatencyMs;
        this.minLatencyMs = minLatencyMs;
        this.maxLatencyMs = maxLatencyMs;
        this.p95LatencyMs = p95LatencyMs;
        this.p99LatencyMs = p99LatencyMs;
    }

    public Monitor getMonitor() {
        return monitor;
    }

    public long getTotalChecks() {
        return totalChecks;
    }

    public long getSuccessChecks() {
        return successChecks;
    }

    public long getFailureChecks() {
        return failureChecks;
    }

    public Double getUptimePercentage() {
        return uptimePercentage;
    }

    public Double getFailureRate() {
        return failureRate;
    }

    public Double getAvgLatencyMs() {
        return avgLatencyMs;
    }

    public Long getMinLatencyMs() {
        return minLatencyMs;
    }

    public Long getMaxLatencyMs() {
        return maxLatencyMs;
    }

    public Long getP95LatencyMs() {
        return p95LatencyMs;
    }

    public Long getP99LatencyMs() {
        return p99LatencyMs;
    }
}
