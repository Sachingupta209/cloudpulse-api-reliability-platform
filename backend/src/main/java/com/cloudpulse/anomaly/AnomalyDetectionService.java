package com.cloudpulse.anomaly;

import com.cloudpulse.exception.ResourceNotFoundException;
import com.cloudpulse.monitor.Monitor;
import com.cloudpulse.monitor.MonitorRepository;
import com.cloudpulse.result.CheckResult;
import com.cloudpulse.result.CheckResultRepository;
import com.cloudpulse.result.CheckStatus;
import com.cloudpulse.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AnomalyDetectionService {

    private static final Logger log = LoggerFactory.getLogger(AnomalyDetectionService.class);

    private final AnomalyRepository anomalyRepository;
    private final CheckResultRepository checkResultRepository;
    private final MonitorRepository monitorRepository;

    public AnomalyDetectionService(
            AnomalyRepository anomalyRepository,
            CheckResultRepository checkResultRepository,
            MonitorRepository monitorRepository) {
        this.anomalyRepository = anomalyRepository;
        this.checkResultRepository = checkResultRepository;
        this.monitorRepository = monitorRepository;
    }

    @Transactional
    public void analyzeCheckResult(Monitor monitor, CheckResult result) {
        if (monitor == null || monitor.getId() == null || monitor.getUser() == null) {
            return;
        }

        try {
            if (result.getStatus() == CheckStatus.SUCCESS) {
                detectLatencySpike(monitor, result);
            } else {
                detectFailureBurst(monitor, result);
            }
            detectUptimeDegradation(monitor);
        } catch (Exception ex) {
            log.warn("Error during anomaly detection for monitor {}: {}", monitor.getId(), ex.getMessage());
        }
    }

    private void detectLatencySpike(Monitor monitor, CheckResult result) {
        long currentLatency = result.getResponseTimeMs();
        if (currentLatency <= 100) {
            // Fast response, skip analysis
            return;
        }

        Instant oneDayAgo = Instant.now().minus(24, ChronoUnit.HOURS);
        List<CheckResult> recentChecks = checkResultRepository.findRecentByMonitorId(monitor.getId(), oneDayAgo);

        List<Long> baselineLatencies = recentChecks.stream()
                .filter(r -> r.getStatus() == CheckStatus.SUCCESS && !r.getId().equals(result.getId()))
                .map(CheckResult::getResponseTimeMs)
                .toList();

        if (baselineLatencies.size() < 5) {
            // Need at least 5 historical samples to establish a statistical baseline
            return;
        }

        double mean = baselineLatencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double variance = baselineLatencies.stream()
                .mapToDouble(l -> Math.pow(l - mean, 2))
                .average()
                .orElse(0.0);
        double stdDev = Math.sqrt(variance);

        // Standard deviation floor to avoid division by zero or extreme sensitivity on ultra-stable pings
        if (stdDev < 15.0) {
            stdDev = 15.0;
        }

        double zScore = (currentLatency - mean) / stdDev;

        if (zScore >= 3.0 || (currentLatency >= 2.5 * mean && currentLatency >= 300)) {
            // Cooling-off check: no latency spike in last 10 minutes
            Instant coolingWindow = Instant.now().minus(10, ChronoUnit.MINUTES);
            if (anomalyRepository.existsRecentByMonitorAndType(monitor.getId(), AnomalyType.LATENCY_SPIKE, coolingWindow)) {
                return;
            }

            AnomalySeverity severity;
            if (zScore >= 5.0 || currentLatency >= 4.0 * mean) {
                severity = AnomalySeverity.CRITICAL;
            } else if (zScore >= 4.0 || currentLatency >= 3.0 * mean) {
                severity = AnomalySeverity.HIGH;
            } else if (zScore >= 3.0 || currentLatency >= 2.5 * mean) {
                severity = AnomalySeverity.MEDIUM;
            } else {
                severity = AnomalySeverity.LOW;
            }

            String description = String.format(
                    "Latency spike: %d ms vs baseline %.1f ms (Z-score: %.2f, +%.0f%%)",
                    currentLatency, mean, zScore, ((currentLatency - mean) / Math.max(1.0, mean)) * 100.0
            );

            Anomaly anomaly = new Anomaly(
                    monitor,
                    monitor.getUser(),
                    AnomalyType.LATENCY_SPIKE,
                    severity,
                    currentLatency,
                    Math.round(mean * 10.0) / 10.0,
                    description,
                    Instant.now()
            );

            anomalyRepository.save(anomaly);
            log.info("Anomaly detected [LATENCY_SPIKE] for monitor '{}': {}", monitor.getName(), description);
        }
    }

    private void detectFailureBurst(Monitor monitor, CheckResult result) {
        int consecutiveFailures = monitor.getConsecutiveFailures();
        if (consecutiveFailures >= 3) {
            // Cooling-off check: no failure burst in last 15 minutes
            Instant coolingWindow = Instant.now().minus(15, ChronoUnit.MINUTES);
            if (anomalyRepository.existsRecentByMonitorAndType(monitor.getId(), AnomalyType.FAILURE_BURST, coolingWindow)) {
                return;
            }

            AnomalySeverity severity = consecutiveFailures >= 5 ? AnomalySeverity.CRITICAL : AnomalySeverity.HIGH;
            String reason = result.getFailureReason() != null ? result.getFailureReason() : "Unhealthy endpoint";
            String description = String.format("Failure burst: %d consecutive failures. Cause: %s", consecutiveFailures, reason);

            Anomaly anomaly = new Anomaly(
                    monitor,
                    monitor.getUser(),
                    AnomalyType.FAILURE_BURST,
                    severity,
                    consecutiveFailures,
                    0.0,
                    description,
                    Instant.now()
            );

            anomalyRepository.save(anomaly);
            log.info("Anomaly detected [FAILURE_BURST] for monitor '{}': {}", monitor.getName(), description);
        }
    }

    private void detectUptimeDegradation(Monitor monitor) {
        Instant oneDayAgo = Instant.now().minus(24, ChronoUnit.HOURS);
        List<CheckResult> checks24h = checkResultRepository.findRecentByMonitorId(monitor.getId(), oneDayAgo);

        if (checks24h.size() < 10) {
            return;
        }

        long total = checks24h.size();
        long success = checks24h.stream().filter(r -> r.getStatus() == CheckStatus.SUCCESS).count();
        double uptime = ((double) success / total) * 100.0;

        if (uptime < 95.0) {
            // Cooling-off check: no uptime degradation in last 30 minutes
            Instant coolingWindow = Instant.now().minus(30, ChronoUnit.MINUTES);
            if (anomalyRepository.existsRecentByMonitorAndType(monitor.getId(), AnomalyType.UPTIME_DEGRADATION, coolingWindow)) {
                return;
            }

            AnomalySeverity severity = uptime < 90.0 ? AnomalySeverity.CRITICAL : AnomalySeverity.HIGH;
            String description = String.format("24-hour uptime degraded to %.1f%% (%d of %d checks successful)",
                    uptime, success, total);

            Anomaly anomaly = new Anomaly(
                    monitor,
                    monitor.getUser(),
                    AnomalyType.UPTIME_DEGRADATION,
                    severity,
                    Math.round(uptime * 100.0) / 100.0,
                    99.0,
                    description,
                    Instant.now()
            );

            anomalyRepository.save(anomaly);
            log.info("Anomaly detected [UPTIME_DEGRADATION] for monitor '{}': {}", monitor.getName(), description);
        }
    }

    @Transactional(readOnly = true)
    public Page<AnomalyDto> getUserAnomalies(Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        return anomalyRepository.findByUserIdWithMonitor(userId, pageable)
                .map(AnomalyDto::from);
    }

    @Transactional(readOnly = true)
    public Page<AnomalyDto> getMonitorAnomalies(Long monitorId, Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        monitorRepository.findByIdAndUserId(monitorId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found with id : '" + monitorId + "'"));

        return anomalyRepository.findByMonitorIdAndUserIdWithMonitor(monitorId, userId, pageable)
                .map(AnomalyDto::from);
    }

    @Transactional
    public AnomalyDto acknowledgeAnomaly(Long anomalyId) {
        Long userId = SecurityUtils.getCurrentUserId();
        Anomaly anomaly = anomalyRepository.findByIdAndUserId(anomalyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Anomaly not found with id : '" + anomalyId + "'"));

        anomaly.acknowledge();
        Anomaly saved = anomalyRepository.save(anomaly);
        return AnomalyDto.from(saved);
    }

    @Transactional(readOnly = true)
    public long getUnacknowledgedCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        return anomalyRepository.countByUserIdAndAcknowledgedFalse(userId);
    }
}
