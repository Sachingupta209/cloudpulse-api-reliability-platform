package com.cloudpulse.analytics;

import com.cloudpulse.incident.IncidentDto;

import java.util.List;

public class DashboardSummaryDto {

    private final long totalMonitors;
    private final long healthyMonitors;
    private final long unhealthyMonitors;
    private final long pausedMonitors;
    private final double overallUptimePercentage;
    private final double averageResponseTimeMs;
    private final long totalChecks;
    private final long failedChecks;
    private final long activeIncidents;
    private final List<IncidentDto> recentIncidents;
    private final List<ResponseTimeTrendDto> responseTimeTrends;

    public DashboardSummaryDto(long totalMonitors, long healthyMonitors, long unhealthyMonitors,
                               long pausedMonitors, double overallUptimePercentage, double averageResponseTimeMs,
                               long totalChecks, long failedChecks, long activeIncidents,
                               List<IncidentDto> recentIncidents, List<ResponseTimeTrendDto> responseTimeTrends) {
        this.totalMonitors = totalMonitors;
        this.healthyMonitors = healthyMonitors;
        this.unhealthyMonitors = unhealthyMonitors;
        this.pausedMonitors = pausedMonitors;
        this.overallUptimePercentage = overallUptimePercentage;
        this.averageResponseTimeMs = averageResponseTimeMs;
        this.totalChecks = totalChecks;
        this.failedChecks = failedChecks;
        this.activeIncidents = activeIncidents;
        this.recentIncidents = recentIncidents;
        this.responseTimeTrends = responseTimeTrends;
    }

    public long getTotalMonitors() {
        return totalMonitors;
    }

    public long getHealthyMonitors() {
        return healthyMonitors;
    }

    public long getUnhealthyMonitors() {
        return unhealthyMonitors;
    }

    public long getPausedMonitors() {
        return pausedMonitors;
    }

    public double getOverallUptimePercentage() {
        return overallUptimePercentage;
    }

    public double getAverageResponseTimeMs() {
        return averageResponseTimeMs;
    }

    public long getTotalChecks() {
        return totalChecks;
    }

    public long getFailedChecks() {
        return failedChecks;
    }

    public long getActiveIncidents() {
        return activeIncidents;
    }

    public List<IncidentDto> getRecentIncidents() {
        return recentIncidents;
    }

    public List<ResponseTimeTrendDto> getResponseTimeTrends() {
        return responseTimeTrends;
    }
}
