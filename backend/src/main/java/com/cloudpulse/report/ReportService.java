package com.cloudpulse.report;

import com.cloudpulse.exception.BadRequestException;
import com.cloudpulse.exception.ResourceNotFoundException;
import com.cloudpulse.incident.Incident;
import com.cloudpulse.incident.IncidentRepository;
import com.cloudpulse.monitor.Monitor;
import com.cloudpulse.monitor.MonitorRepository;
import com.cloudpulse.result.CheckResult;
import com.cloudpulse.result.CheckResultRepository;
import com.cloudpulse.result.CheckStatus;
import com.cloudpulse.security.SecurityUtils;
import com.cloudpulse.user.User;
import com.cloudpulse.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class ReportService {

    private final MonitorRepository monitorRepository;
    private final CheckResultRepository checkResultRepository;
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final PdfReportService pdfReportService;

    public ReportService(
            MonitorRepository monitorRepository,
            CheckResultRepository checkResultRepository,
            IncidentRepository incidentRepository,
            UserRepository userRepository,
            PdfReportService pdfReportService) {
        this.monitorRepository = monitorRepository;
        this.checkResultRepository = checkResultRepository;
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.pdfReportService = pdfReportService;
    }

    @Transactional(readOnly = true)
    public byte[] generateMonitorReport(Long monitorId, Instant start, Instant end) {
        Long userId = SecurityUtils.getCurrentUserId();
        Monitor monitor = monitorRepository.findByIdAndUserId(monitorId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found with id : '" + monitorId + "'"));

        DateRange range = validateDateRange(start, end);
        MonitorStats stats = computeMonitorStats(monitor, range.start, range.end);
        List<Incident> incidents = incidentRepository.findByMonitorIdAndStartedAtBetween(monitor.getId(), range.start, range.end);

        return pdfReportService.generateSingleMonitorReport(monitor, range.start, range.end, stats, incidents);
    }

    @Transactional(readOnly = true)
    public byte[] generateSummaryReport(Instant start, Instant end) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id : '" + userId + "'"));

        DateRange range = validateDateRange(start, end);
        List<Monitor> monitors = monitorRepository.findByUserId(userId);

        List<MonitorStats> monitorStats = new ArrayList<>();
        for (Monitor m : monitors) {
            monitorStats.add(computeMonitorStats(m, range.start, range.end));
        }

        List<Incident> incidents = incidentRepository.findByUserIdAndStartedAtBetween(userId, range.start, range.end);
        return pdfReportService.generateSummaryReport(user, range.start, range.end, monitorStats, incidents);
    }

    @Transactional(readOnly = true)
    public byte[] generateIncidentReport(Instant start, Instant end) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id : '" + userId + "'"));

        DateRange range = validateDateRange(start, end);
        List<Incident> incidents = incidentRepository.findByUserIdAndStartedAtBetween(userId, range.start, range.end);

        return pdfReportService.generateIncidentReport(user, range.start, range.end, incidents);
    }

    @Transactional(readOnly = true)
    public byte[] generatePerformanceReport(Instant start, Instant end) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id : '" + userId + "'"));

        DateRange range = validateDateRange(start, end);
        List<Monitor> monitors = monitorRepository.findByUserId(userId);

        List<MonitorStats> monitorStats = new ArrayList<>();
        for (Monitor m : monitors) {
            monitorStats.add(computeMonitorStats(m, range.start, range.end));
        }

        return pdfReportService.generatePerformanceReport(user, range.start, range.end, monitorStats);
    }

    public MonitorStats computeMonitorStats(Monitor monitor, Instant start, Instant end) {
        List<CheckResult> results = checkResultRepository.findByMonitorIdAndCheckedAtBetween(monitor.getId(), start, end);

        long total = results.size();
        if (total == 0) {
            return new MonitorStats(monitor, 0, 0, 0, null, null, null, null, null, null, null);
        }

        long success = 0;
        long failure = 0;
        List<Long> latencies = new ArrayList<>();

        for (CheckResult r : results) {
            if (r.getStatus() == CheckStatus.SUCCESS) {
                success++;
                latencies.add(r.getResponseTimeMs());
            } else if (r.getStatus() == CheckStatus.FAILURE) {
                failure++;
            }
        }

        Double uptime = (total > 0) ? Math.round(((double) success / total) * 10000.0) / 100.0 : null;
        Double failureRate = (total > 0) ? Math.round(((double) failure / total) * 10000.0) / 100.0 : null;

        Collections.sort(latencies);
        Double avgLatency = null;
        Long minLatency = null;
        Long maxLatency = null;
        Long p95 = null;
        Long p99 = null;

        if (!latencies.isEmpty()) {
            avgLatency = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
            avgLatency = Math.round(avgLatency * 10.0) / 10.0;
            minLatency = latencies.get(0);
            maxLatency = latencies.get(latencies.size() - 1);

            int idx95 = (int) Math.ceil(0.95 * latencies.size()) - 1;
            int idx99 = (int) Math.ceil(0.99 * latencies.size()) - 1;
            p95 = latencies.get(Math.max(0, idx95));
            p99 = latencies.get(Math.max(0, idx99));
        }

        return new MonitorStats(monitor, total, success, failure, uptime, failureRate, avgLatency, minLatency, maxLatency, p95, p99);
    }

    private DateRange validateDateRange(Instant start, Instant end) {
        Instant actualEnd = end != null ? end : Instant.now();
        Instant actualStart = start != null ? start : actualEnd.minus(30, ChronoUnit.DAYS);

        if (actualStart.isAfter(actualEnd)) {
            throw new BadRequestException("Start date cannot be after end date");
        }

        // Limit maximum date range to 365 days for performance
        if (ChronoUnit.DAYS.between(actualStart, actualEnd) > 365) {
            throw new BadRequestException("Date range cannot exceed 365 days");
        }

        return new DateRange(actualStart, actualEnd);
    }

    private record DateRange(Instant start, Instant end) {}
}
