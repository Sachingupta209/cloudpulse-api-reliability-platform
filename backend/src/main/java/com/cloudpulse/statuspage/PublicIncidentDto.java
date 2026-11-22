package com.cloudpulse.statuspage;

import java.time.Instant;

public class PublicIncidentDto {

    private String monitorName;
    private String title;
    private String status;
    private Instant startedAt;
    private Instant resolvedAt;
    private Long durationMinutes;

    public PublicIncidentDto() {
    }

    public PublicIncidentDto(String monitorName, String title, String status, Instant startedAt, Instant resolvedAt, Long durationMinutes) {
        this.monitorName = monitorName;
        this.title = title;
        this.status = status;
        this.startedAt = startedAt;
        this.resolvedAt = resolvedAt;
        this.durationMinutes = durationMinutes;
    }

    public String getMonitorName() {
        return monitorName;
    }

    public String getTitle() {
        return title;
    }

    public String getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public Long getDurationMinutes() {
        return durationMinutes;
    }
}
