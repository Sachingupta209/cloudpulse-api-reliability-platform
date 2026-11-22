package com.cloudpulse.anomaly;

import java.time.Instant;

public record AnomalyDto(
        Long id,
        Long monitorId,
        String monitorName,
        String monitorUrl,
        AnomalyType anomalyType,
        AnomalySeverity severity,
        double observedValue,
        double baselineValue,
        String description,
        Instant detectedAt,
        boolean acknowledged,
        Instant acknowledgedAt
) {
    public static AnomalyDto from(Anomaly anomaly) {
        return new AnomalyDto(
                anomaly.getId(),
                anomaly.getMonitor().getId(),
                anomaly.getMonitor().getName(),
                anomaly.getMonitor().getUrl(),
                anomaly.getAnomalyType(),
                anomaly.getSeverity(),
                anomaly.getObservedValue(),
                anomaly.getBaselineValue(),
                anomaly.getDescription(),
                anomaly.getDetectedAt(),
                anomaly.isAcknowledged(),
                anomaly.getAcknowledgedAt()
        );
    }
}
