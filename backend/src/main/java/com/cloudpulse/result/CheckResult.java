package com.cloudpulse.result;

import com.cloudpulse.monitor.Monitor;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "check_results")
public class CheckResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "monitor_id", nullable = false)
    private Monitor monitor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheckStatus status;

    @Column(name = "http_status_code")
    private Integer httpStatusCode;

    @Column(name = "response_time_ms", nullable = false)
    private long responseTimeMs;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "validation_details_json", columnDefinition = "TEXT")
    private String validationDetailsJson;

    @Column(name = "checked_at", nullable = false, updatable = false)
    private Instant checkedAt = Instant.now();

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    public CheckResult() {
    }

    public CheckResult(Monitor monitor, CheckStatus status, Integer httpStatusCode, long responseTimeMs,
                       String failureReason, String validationDetailsJson, String ipAddress) {
        this.monitor = monitor;
        this.status = status;
        this.httpStatusCode = httpStatusCode;
        this.responseTimeMs = responseTimeMs;
        this.failureReason = failureReason;
        this.validationDetailsJson = validationDetailsJson;
        this.ipAddress = ipAddress;
        this.checkedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Monitor getMonitor() {
        return monitor;
    }

    public void setMonitor(Monitor monitor) {
        this.monitor = monitor;
    }

    public CheckStatus getStatus() {
        return status;
    }

    public void setStatus(CheckStatus status) {
        this.status = status;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHttpStatusCode(Integer httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getValidationDetailsJson() {
        return validationDetailsJson;
    }

    public void setValidationDetailsJson(String validationDetailsJson) {
        this.validationDetailsJson = validationDetailsJson;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(Instant checkedAt) {
        this.checkedAt = checkedAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}
