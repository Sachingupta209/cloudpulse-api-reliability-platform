package com.cloudpulse.analytics;

import com.cloudpulse.anomaly.AnomalyDto;

import java.util.List;

public record AdvancedAnalyticsDto(
        double overallUptimePercentage,
        double failureRatePercentage,
        Double mttrMinutes,
        Double mtbfHours,
        Double avgLatencyMs,
        Long minLatencyMs,
        Long maxLatencyMs,
        Long p95LatencyMs,
        Long p99LatencyMs,
        long totalIncidents,
        long activeIncidents,
        long totalAnomalies,
        long unacknowledgedAnomalies,
        Double uptime24h,
        Double uptime7d,
        Double uptime30d,
        List<AnomalyDto> recentAnomalies,
        List<ResponseTimeTrendDto> trends
) {
}
