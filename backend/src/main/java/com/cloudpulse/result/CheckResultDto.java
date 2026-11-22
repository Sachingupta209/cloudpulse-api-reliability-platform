package com.cloudpulse.result;

import java.time.Instant;

public class CheckResultDto {

    private final Long id;
    private final Long monitorId;
    private final CheckStatus status;
    private final Integer httpStatusCode;
    private final long responseTimeMs;
    private final String failureReason;
    private final String validationDetailsJson;
    private final Instant checkedAt;
    private final String ipAddress;

    public CheckResultDto(Long id, Long monitorId, CheckStatus status, Integer httpStatusCode,
                          long responseTimeMs, String failureReason, String validationDetailsJson,
                          Instant checkedAt, String ipAddress) {
        this.id = id;
        this.monitorId = monitorId;
        this.status = status;
        this.httpStatusCode = httpStatusCode;
        this.responseTimeMs = responseTimeMs;
        this.failureReason = failureReason;
        this.validationDetailsJson = validationDetailsJson;
        this.checkedAt = checkedAt;
        this.ipAddress = ipAddress;
    }

    public static CheckResultDto from(CheckResult result) {
        return new CheckResultDto(
                result.getId(),
                result.getMonitor().getId(),
                result.getStatus(),
                result.getHttpStatusCode(),
                result.getResponseTimeMs(),
                result.getFailureReason(),
                result.getValidationDetailsJson(),
                result.getCheckedAt(),
                result.getIpAddress()
        );
    }

    public Long getId() {
        return id;
    }

    public Long getMonitorId() {
        return monitorId;
    }

    public CheckStatus getStatus() {
        return status;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getValidationDetailsJson() {
        return validationDetailsJson;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }
}
