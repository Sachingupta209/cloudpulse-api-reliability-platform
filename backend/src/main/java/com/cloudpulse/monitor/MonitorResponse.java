package com.cloudpulse.monitor;

import com.cloudpulse.validation.ValidationRuleDto;

import java.time.Instant;
import java.util.List;

public class MonitorResponse {

    private final Long id;
    private final String name;
    private final String url;
    private final HttpMethod httpMethod;
    private final int checkIntervalSeconds;
    private final int timeoutMs;
    private final Integer expectedStatusCode;
    private final String headersJson;
    private final String requestBody;
    private final MonitorStatus status;
    private final HealthStatus healthStatus;
    private final int consecutiveFailures;
    private final Instant lastCheckedAt;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final List<ValidationRuleDto> validationRules;
    private final Double uptimePercentage;
    private final Double avgResponseTimeMs;

    public MonitorResponse(Long id, String name, String url, HttpMethod httpMethod, int checkIntervalSeconds,
                           int timeoutMs, Integer expectedStatusCode, String headersJson, String requestBody,
                           MonitorStatus status, HealthStatus healthStatus, int consecutiveFailures,
                           Instant lastCheckedAt, Instant createdAt, Instant updatedAt,
                           List<ValidationRuleDto> validationRules, Double uptimePercentage, Double avgResponseTimeMs) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.httpMethod = httpMethod;
        this.checkIntervalSeconds = checkIntervalSeconds;
        this.timeoutMs = timeoutMs;
        this.expectedStatusCode = expectedStatusCode;
        this.headersJson = headersJson;
        this.requestBody = requestBody;
        this.status = status;
        this.healthStatus = healthStatus;
        this.consecutiveFailures = consecutiveFailures;
        this.lastCheckedAt = lastCheckedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.validationRules = validationRules;
        this.uptimePercentage = uptimePercentage;
        this.avgResponseTimeMs = avgResponseTimeMs;
    }

    public static MonitorResponse from(Monitor monitor, Double uptimePercentage, Double avgResponseTimeMs) {
        List<ValidationRuleDto> rules = monitor.getValidationRules() != null
                ? monitor.getValidationRules().stream().map(ValidationRuleDto::from).toList()
                : List.of();

        return new MonitorResponse(
                monitor.getId(),
                monitor.getName(),
                monitor.getUrl(),
                monitor.getHttpMethod(),
                monitor.getCheckIntervalSeconds(),
                monitor.getTimeoutMs(),
                monitor.getExpectedStatusCode(),
                monitor.getHeadersJson(),
                monitor.getRequestBody(),
                monitor.getStatus(),
                monitor.getHealthStatus(),
                monitor.getConsecutiveFailures(),
                monitor.getLastCheckedAt(),
                monitor.getCreatedAt(),
                monitor.getUpdatedAt(),
                rules,
                uptimePercentage,
                avgResponseTimeMs
        );
    }

    public static MonitorResponse from(Monitor monitor) {
        return from(monitor, null, null);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public int getCheckIntervalSeconds() {
        return checkIntervalSeconds;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public Integer getExpectedStatusCode() {
        return expectedStatusCode;
    }

    public String getHeadersJson() {
        return headersJson;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public MonitorStatus getStatus() {
        return status;
    }

    public HealthStatus getHealthStatus() {
        return healthStatus;
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public Instant getLastCheckedAt() {
        return lastCheckedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<ValidationRuleDto> getValidationRules() {
        return validationRules;
    }

    public Double getUptimePercentage() {
        return uptimePercentage;
    }

    public Double getAvgResponseTimeMs() {
        return avgResponseTimeMs;
    }
}
