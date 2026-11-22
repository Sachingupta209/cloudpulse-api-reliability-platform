package com.cloudpulse.statuspage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public class StatusPageRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title cannot exceed 100 characters")
    private String title;

    private String description;

    @Pattern(regexp = "^[a-z0-9][a-z0-9-]{1,48}[a-z0-9]$|^$", message = "Slug must be lowercase alphanumeric with hyphens (3-50 chars)")
    private String slug;

    private Boolean isPublished;

    private List<Long> monitorIds;

    public StatusPageRequest() {
    }

    public StatusPageRequest(String title, String description, String slug, Boolean isPublished, List<Long> monitorIds) {
        this.title = title;
        this.description = description;
        this.slug = slug;
        this.isPublished = isPublished;
        this.monitorIds = monitorIds;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public Boolean getIsPublished() {
        return isPublished;
    }

    public void setIsPublished(Boolean isPublished) {
        this.isPublished = isPublished;
    }

    public List<Long> getMonitorIds() {
        return monitorIds;
    }

    public void setMonitorIds(List<Long> monitorIds) {
        this.monitorIds = monitorIds;
    }
}
