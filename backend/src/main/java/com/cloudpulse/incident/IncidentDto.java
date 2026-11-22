package com.cloudpulse.incident;

import java.time.Instant;

public class IncidentDto {

    private final Long id;
    private final Long monitorId;
    private final String monitorName;
    private final String monitorUrl;
    private final IncidentStatus status;
    private final String title;
    private final String rootCause;
    private final Instant startedAt;
    private final Instant resolvedAt;
    private final Long downtimeSeconds;
    private final Instant createdAt;

    public IncidentDto(Long id, Long monitorId, String monitorName, String monitorUrl,
                       IncidentStatus status, String title, String rootCause,
                       Instant startedAt, Instant resolvedAt, Long downtimeSeconds, Instant createdAt) {
        this.id = id;
        this.monitorId = monitorId;
        this.monitorName = monitorName;
        this.monitorUrl = monitorUrl;
        this.status = status;
        this.title = title;
        this.rootCause = rootCause;
        this.startedAt = startedAt;
        this.resolvedAt = resolvedAt;
        this.downtimeSeconds = downtimeSeconds;
        this.createdAt = createdAt;
    }

    public static IncidentDto from(Incident incident) {
        return new IncidentDto(
                incident.getId(),
                incident.getMonitor().getId(),
                incident.getMonitor().getName(),
                incident.getMonitor().getUrl(),
                incident.getStatus(),
                incident.getTitle(),
                incident.getRootCause(),
                incident.getStartedAt(),
                incident.getResolvedAt(),
                incident.getDowntimeSeconds(),
                incident.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public Long getMonitorId() {
        return monitorId;
    }

    public String getMonitorName() {
        return monitorName;
    }

    public String getMonitorUrl() {
        return monitorUrl;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    public String getRootCause() {
        return rootCause;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public Long getDowntimeSeconds() {
        return downtimeSeconds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
