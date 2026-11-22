package com.cloudpulse.statuspage;

import java.time.Instant;
import java.util.List;

public class StatusPageResponse {

    private Long id;
    private String title;
    private String description;
    private String slug;
    @com.fasterxml.jackson.annotation.JsonProperty("isPublished")
    private boolean isPublished;
    private List<StatusPageMonitorDto> monitors;
    private Instant createdAt;
    private Instant updatedAt;

    public StatusPageResponse() {
    }

    public StatusPageResponse(Long id, String title, String description, String slug, boolean isPublished,
                              List<StatusPageMonitorDto> monitors, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.slug = slug;
        this.isPublished = isPublished;
        this.monitors = monitors;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getSlug() {
        return slug;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("isPublished")
    public boolean isPublished() {
        return isPublished;
    }

    public List<StatusPageMonitorDto> getMonitors() {
        return monitors;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public static class StatusPageMonitorDto {
        private Long monitorId;
        private String name;
        private String status;
        private String healthStatus;
        private int displayOrder;

        public StatusPageMonitorDto() {
        }

        public StatusPageMonitorDto(Long monitorId, String name, String status, String healthStatus, int displayOrder) {
            this.monitorId = monitorId;
            this.name = name;
            this.status = status;
            this.healthStatus = healthStatus;
            this.displayOrder = displayOrder;
        }

        public Long getMonitorId() {
            return monitorId;
        }

        public String getName() {
            return name;
        }

        public String getStatus() {
            return status;
        }

        public String getHealthStatus() {
            return healthStatus;
        }

        public int getDisplayOrder() {
            return displayOrder;
        }
    }
}
