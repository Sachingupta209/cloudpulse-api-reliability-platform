package com.cloudpulse.analytics;

import com.cloudpulse.anomaly.AnomalyDto;
import com.cloudpulse.anomaly.AnomalyRepository;
import com.cloudpulse.exception.ResourceNotFoundException;
import com.cloudpulse.incident.Incident;
import com.cloudpulse.incident.IncidentDto;
import com.cloudpulse.incident.IncidentRepository;
import com.cloudpulse.incident.IncidentStatus;
import com.cloudpulse.monitor.HealthStatus;
import com.cloudpulse.monitor.Monitor;
import com.cloudpulse.monitor.MonitorRepository;
import com.cloudpulse.monitor.MonitorStatus;
import com.cloudpulse.result.CheckResult;
import com.cloudpulse.result.CheckResultRepository;
import com.cloudpulse.result.CheckStatus;
import com.cloudpulse.security.SecurityUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class AnalyticsService {

    private final MonitorRepository monitorRepository;
    private final CheckResultRepository checkResultRepository;
    private final IncidentRepository incidentRepository;
    private final AnomalyRepository anomalyRepository;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneOffset.UTC);

    public AnalyticsService(
            MonitorRepository monitorRepository,
            CheckResultRepository checkResultRepository,
            IncidentRepository incidentRepository,
            AnomalyRepository anomalyRepository) {
        this.monitorRepository = monitorRepository;
        this.checkResultRepository = checkResultRepository;
        this.incidentRepository = incidentRepository;
        this.anomalyRepository = anomalyRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryDto getDashboardSummary() {
        Long userId = SecurityUtils.getCurrentUserId();

        long totalMonitors = monitorRepository.countByUserId(userId);
        long healthyMonitors = monitorRepository.countByUserIdAndHealthStatus(userId, HealthStatus.HEALTHY);
        long unhealthyMonitors = monitorRepository.countByUserIdAndHealthStatus(userId, HealthStatus.UNHEALTHY);
        long pausedMonitors = monitorRepository.countByUserIdAndStatus(userId, MonitorStatus.PAUSED);

        long totalChecks = checkResultRepository.countByUserId(userId);
        long failedChecks = checkResultRepository.countFailuresByUserId(userId);
        long successfulChecks = totalChecks - failedChecks;

        double overallUptimePercentage = (totalChecks > 0)
                ? ((double) successfulChecks / totalChecks) * 100.0
                : 100.0;

        Double avgLatency = checkResultRepository.findAverageResponseTimeByUserId(userId);
        double averageResponseTimeMs = (avgLatency != null) ? Math.round(avgLatency * 100.0) / 100.0 : 0.0;

        long activeIncidents = incidentRepository.countByUserIdAndStatus(userId, IncidentStatus.OPEN);

        List<IncidentDto> recentIncidents = incidentRepository.findTop5ByUserIdOrderByStartedAtDesc(userId)
                .stream()
                .map(IncidentDto::from)
                .toList();

        List<ResponseTimeTrendDto> trends = calculateResponseTimeTrends(userId, 24);

        return new DashboardSummaryDto(
                totalMonitors,
                healthyMonitors,
                unhealthyMonitors,
                pausedMonitors,
                Math.round(overallUptimePercentage * 100.0) / 100.0,
                averageResponseTimeMs,
                totalChecks,
                failedChecks,
                activeIncidents,
                recentIncidents,
                trends
        );
    }

    @Transactional(readOnly = true)
    public AdvancedAnalyticsDto getAdvancedAnalytics() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Monitor> monitors = monitorRepository.findByUserId(userId);
        List<Long> monitorIds = monitors.stream().map(Monitor::getId).toList();

        long totalChecks = checkResultRepository.countByUserId(userId);
        long failedChecks = checkResultRepository.countFailuresByUserId(userId);
        long successfulChecks = totalChecks - failedChecks;

        double overallUptime = (totalChecks > 0)
                ? Math.round(((double) successfulChecks / totalChecks) * 10000.0) / 100.0
                : 100.0;
        double failureRate = (totalChecks > 0)
                ? Math.round(((double) failedChecks / totalChecks) * 10000.0) / 100.0
                : 0.0;

        // MTTR calculation
        List<Incident> resolvedIncidents = incidentRepository.findByUserIdAndStatus(userId, IncidentStatus.RESOLVED);
        Double mttrMinutes = null;
        long totalDowntimeMinutes = 0;
        if (!resolvedIncidents.isEmpty()) {
            long totalDuration = 0;
            for (Incident inc : resolvedIncidents) {
                if (inc.getStartedAt() != null && inc.getResolvedAt() != null) {
                    long minutes = Math.max(1, Duration.between(inc.getStartedAt(), inc.getResolvedAt()).toMinutes());
                    totalDuration += minutes;
                }
            }
            totalDowntimeMinutes = totalDuration;
            mttrMinutes = Math.round((totalDuration / (double) resolvedIncidents.size()) * 10.0) / 10.0;
        }

        // MTBF calculation (hours)
        long totalIncidents = incidentRepository.countByUserId(userId);
        Double mtbfHours = null;
        if (totalIncidents > 0 && !monitors.isEmpty()) {
            double totalOperatingHours = monitors.size() * 720.0; // 30-day window
            double downtimeHours = totalDowntimeMinutes / 60.0;
            double uptimeHours = Math.max(0.0, totalOperatingHours - downtimeHours);
            mtbfHours = Math.round((uptimeHours / totalIncidents) * 10.0) / 10.0;
        }

        // Latencies and percentiles over past 7 days
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        List<Long> latencies = new ArrayList<>();
        for (Monitor m : monitors) {
            List<CheckResult> results = checkResultRepository.findRecentByMonitorId(m.getId(), sevenDaysAgo);
            for (CheckResult r : results) {
                if (r.getStatus() == CheckStatus.SUCCESS) {
                    latencies.add(r.getResponseTimeMs());
                }
            }
        }

        Double avgLatency = null;
        Long minLatency = null;
        Long maxLatency = null;
        Long p95 = null;
        Long p99 = null;

        if (!latencies.isEmpty()) {
            Collections.sort(latencies);
            double avg = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
            avgLatency = Math.round(avg * 10.0) / 10.0;
            minLatency = latencies.get(0);
            maxLatency = latencies.get(latencies.size() - 1);
            int idx95 = (int) Math.ceil(0.95 * latencies.size()) - 1;
            int idx99 = (int) Math.ceil(0.99 * latencies.size()) - 1;
            p95 = latencies.get(Math.max(0, idx95));
            p99 = latencies.get(Math.max(0, idx99));
        }

        // Window uptimes (24h, 7d, 30d)
        Double uptime24h = calculateWindowUptime(monitorIds, Instant.now().minus(24, ChronoUnit.HOURS));
        Double uptime7d = calculateWindowUptime(monitorIds, sevenDaysAgo);
        Double uptime30d = calculateWindowUptime(monitorIds, Instant.now().minus(30, ChronoUnit.DAYS));

        long activeIncidents = incidentRepository.countByUserIdAndStatus(userId, IncidentStatus.OPEN);
        long totalAnomalies = anomalyRepository.count();
        long unackAnomalies = anomalyRepository.countByUserIdAndAcknowledgedFalse(userId);

        List<AnomalyDto> recentAnomalies = anomalyRepository.findByUserIdWithMonitor(userId, PageRequest.of(0, 10))
                .getContent()
                .stream()
                .map(AnomalyDto::from)
                .toList();

        List<ResponseTimeTrendDto> trends = calculateResponseTimeTrends(userId, 24);

        return new AdvancedAnalyticsDto(
                overallUptime,
                failureRate,
                mttrMinutes,
                mtbfHours,
                avgLatency,
                minLatency,
                maxLatency,
                p95,
                p99,
                totalIncidents,
                activeIncidents,
                totalAnomalies,
                unackAnomalies,
                uptime24h,
                uptime7d,
                uptime30d,
                recentAnomalies,
                trends
        );
    }

    private Double calculateWindowUptime(List<Long> monitorIds, Instant since) {
        if (monitorIds.isEmpty()) return null;
        List<Object[]> rows = checkResultRepository.aggregateStatsByMonitorIdsSince(monitorIds, since);
        long total = 0;
        long success = 0;
        for (Object[] row : rows) {
            total += ((Number) row[1]).longValue();
            if (row[2] != null) {
                success += ((Number) row[2]).longValue();
            }
        }
        if (total == 0) return null;
        return Math.round(((double) success / total) * 10000.0) / 100.0;
    }

    @Transactional(readOnly = true)
    public List<ResponseTimeTrendDto> calculateResponseTimeTrends(Long userId, int hours) {
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        List<Monitor> userMonitors = monitorRepository.findByUserId(userId);

        if (userMonitors.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, List<CheckResult>> bucketMap = new TreeMap<>();
        for (Monitor monitor : userMonitors) {
            List<CheckResult> results = checkResultRepository.findRecentByMonitorId(monitor.getId(), since);
            for (CheckResult r : results) {
                Instant hourInstant = r.getCheckedAt().truncatedTo(ChronoUnit.HOURS);
                String label = TIME_FORMATTER.format(hourInstant);
                bucketMap.computeIfAbsent(label, k -> new ArrayList<>()).add(r);
            }
        }

        List<ResponseTimeTrendDto> trendList = new ArrayList<>();
        for (Map.Entry<String, List<CheckResult>> entry : bucketMap.entrySet()) {
            List<CheckResult> list = entry.getValue();
            long total = list.size();
            long success = list.stream().filter(r -> r.getStatus() == CheckStatus.SUCCESS).count();
            double avgLatency = list.stream()
                    .filter(r -> r.getStatus() == CheckStatus.SUCCESS)
                    .mapToLong(CheckResult::getResponseTimeMs)
                    .average()
                    .orElse(0.0);

            trendList.add(new ResponseTimeTrendDto(
                    entry.getKey(),
                    Math.round(avgLatency * 100.0) / 100.0,
                    total,
                    success
            ));
        }

        return trendList;
    }
}
