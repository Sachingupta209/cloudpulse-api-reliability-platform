package com.cloudpulse.statuspage;

import java.time.Instant;
import java.util.List;

public class PublicStatusPageDto {

    private String title;
    private String description;
    private PublicSystemStatus overallStatus;
    private Instant updatedAt;
    private List<PublicMonitorDto> monitors;
    private List<PublicIncidentDto> recentIncidents;

    public PublicStatusPageDto() {
    }

    public PublicStatusPageDto(String title, String description, PublicSystemStatus overallStatus,
                               Instant updatedAt, List<PublicMonitorDto> monitors, List<PublicIncidentDto> recentIncidents) {
        this.title = title;
        this.description = description;
        this.overallStatus = overallStatus;
        this.updatedAt = updatedAt;
        this.monitors = monitors;
        this.recentIncidents = recentIncidents;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public PublicSystemStatus getOverallStatus() {
        return overallStatus;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<PublicMonitorDto> getMonitors() {
        return monitors;
    }

    public List<PublicIncidentDto> getRecentIncidents() {
        return recentIncidents;
    }
}
