package com.cloudpulse.monitoring;

import com.cloudpulse.anomaly.AnomalyDetectionService;
import com.cloudpulse.incident.IncidentService;
import com.cloudpulse.monitor.HealthStatus;
import com.cloudpulse.monitor.Monitor;
import com.cloudpulse.monitor.MonitorRepository;
import com.cloudpulse.notification.NotificationService;
import com.cloudpulse.result.CheckResult;
import com.cloudpulse.result.CheckResultRepository;
import com.cloudpulse.result.CheckStatus;
import com.cloudpulse.validation.ValidationEngine;
import com.cloudpulse.validation.ValidationResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class MonitoringService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);

    private final MonitoringHttpClient httpClient;
    private final ValidationEngine validationEngine;
    private final CheckResultRepository checkResultRepository;
    private final MonitorRepository monitorRepository;
    private final IncidentService incidentService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final int failureThreshold;

    @Autowired(required = false)
    private AnomalyDetectionService anomalyDetectionService;

    public void setAnomalyDetectionService(AnomalyDetectionService anomalyDetectionService) {
        this.anomalyDetectionService = anomalyDetectionService;
    }

    public MonitoringService(
            MonitoringHttpClient httpClient,
            ValidationEngine validationEngine,
            CheckResultRepository checkResultRepository,
            MonitorRepository monitorRepository,
            IncidentService incidentService,
            NotificationService notificationService,
            ObjectMapper objectMapper,
            @Value("${app.monitoring.failure-threshold-for-incident:3}") int failureThreshold) {
        this.httpClient = httpClient;
        this.validationEngine = validationEngine;
        this.checkResultRepository = checkResultRepository;
        this.monitorRepository = monitorRepository;
        this.incidentService = incidentService;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
        this.failureThreshold = failureThreshold;
    }

    @Transactional
    public CheckResult executeCheckById(Long monitorId) {
        log.debug("Executing scheduled health check for monitor id={}", monitorId);
        Monitor monitor = monitorRepository.findByIdWithValidationRules(monitorId).orElse(null);
        if (monitor == null) {
            log.warn("Monitor id={} not found for scheduled check", monitorId);
            return null;
        }
        if (monitor.getStatus() != com.cloudpulse.monitor.MonitorStatus.ACTIVE) {
            log.debug("Monitor id={} is not ACTIVE (status={}), skipping check", monitorId, monitor.getStatus());
            return null;
        }
        return performCheck(monitor);
    }

    @Transactional
    public CheckResult executeCheck(Monitor monitor) {
        if (monitor == null) {
            throw new IllegalArgumentException("Monitor cannot be null");
        }
        // If entity is already loaded but might be detached or have uninitialized validationRules,
        // re-fetch it within this transaction boundary using JOIN FETCH
        Monitor targetMonitor = monitor;
        if (monitor.getId() != null) {
            targetMonitor = monitorRepository.findByIdWithValidationRules(monitor.getId())
                    .orElse(monitor);
        }
        return performCheck(targetMonitor);
    }

    private CheckResult performCheck(Monitor monitor) {
        log.debug("Executing health check for monitor id={} ('{}') at {}", monitor.getId(), monitor.getName(), monitor.getUrl());

        Map<String, String> customHeaders = null;
        if (monitor.getHeadersJson() != null && !monitor.getHeadersJson().isBlank()) {
            try {
                customHeaders = objectMapper.readValue(monitor.getHeadersJson(), new TypeReference<>() {});
            } catch (Exception ex) {
                log.warn("Failed to parse headers JSON for monitor id={}: {}", monitor.getId(), ex.getMessage());
            }
        }

        HttpResponseSnapshot snapshot = httpClient.execute(
                monitor.getUrl(),
                monitor.getHttpMethod(),
                monitor.getTimeoutMs(),
                customHeaders,
                monitor.getRequestBody()
        );

        boolean isOverallHealthy = true;
        String failureReason = null;
        List<ValidationResult> validationResults = null;

        if (!snapshot.isSuccessfulConnection()) {
            isOverallHealthy = false;
            failureReason = snapshot.getErrorMessage();
        } else {
            // Check default HTTP status code if configured
            if (monitor.getExpectedStatusCode() != null && snapshot.getStatusCode() != monitor.getExpectedStatusCode()) {
                isOverallHealthy = false;
                failureReason = String.format("Expected HTTP %d, but endpoint returned %d",
                        monitor.getExpectedStatusCode(), snapshot.getStatusCode());
            }

            // Run Smart API Validation Rules (JSONPath, Response Time, Headers, etc.)
            validationResults = validationEngine.validate(
                    snapshot.getStatusCode(),
                    snapshot.getResponseTimeMs(),
                    snapshot.getHeaders(),
                    snapshot.getBody(),
                    monitor.getValidationRules()
            );

            for (ValidationResult vr : validationResults) {
                if (!vr.isPassed()) {
                    isOverallHealthy = false;
                    if (failureReason == null) {
                        failureReason = vr.getFailureReason();
                    } else {
                        failureReason += "; " + vr.getFailureReason();
                    }
                }
            }
        }

        // Update Monitor state
        HealthStatus previousHealth = monitor.getHealthStatus();
        monitor.setLastCheckedAt(Instant.now());
        java.util.Optional<com.cloudpulse.incident.Incident> incidentResult;
        if (isOverallHealthy) {
            monitor.setHealthStatus(HealthStatus.HEALTHY);
            monitor.setConsecutiveFailures(0);
            incidentResult = incidentService.handleRecovery(monitor);
        } else {
            monitor.setHealthStatus(HealthStatus.UNHEALTHY);
            monitor.setConsecutiveFailures(monitor.getConsecutiveFailures() + 1);
            incidentResult = incidentService.handleFailure(monitor, failureReason, monitor.getConsecutiveFailures(), failureThreshold);
        }
        monitorRepository.save(monitor);

        // Serialize validation details
        String validationDetailsJson = null;
        try {
            if (validationResults != null && !validationResults.isEmpty()) {
                validationDetailsJson = objectMapper.writeValueAsString(validationResults);
            }
        } catch (Exception ex) {
            log.warn("Failed to serialize validation results for monitor id={}: {}", monitor.getId(), ex.getMessage());
        }

        CheckResult result = new CheckResult(
                monitor,
                isOverallHealthy ? CheckStatus.SUCCESS : CheckStatus.FAILURE,
                snapshot.getStatusCode() > 0 ? snapshot.getStatusCode() : null,
                snapshot.getResponseTimeMs(),
                failureReason,
                validationDetailsJson,
                snapshot.getIpAddress()
        );

        CheckResult savedResult = checkResultRepository.save(result);

        // Execute live anomaly detection
        if (anomalyDetectionService != null) {
            try {
                anomalyDetectionService.analyzeCheckResult(monitor, savedResult);
            } catch (Exception ex) {
                log.warn("Anomaly detection failed for monitor id={}: {}", monitor.getId(), ex.getMessage());
            }
        }

        // Trigger notifications for health state changes with clean alert suppression
        if (isOverallHealthy) {
            // When an incident was resolved, INCIDENT_RESOLVED is sent with full downtime metrics.
            // Suppress duplicate SERVICE_RECOVERED alert. Only send SERVICE_RECOVERED for transient recovery.
            if (previousHealth == HealthStatus.UNHEALTHY && incidentResult.isEmpty()) {
                notificationService.notifyServiceRecovered(monitor, savedResult.getId());
            }
        } else {
            // When an incident was opened, INCIDENT_OPENED is sent.
            // Suppress duplicate SERVICE_UNHEALTHY alert. Only send SERVICE_UNHEALTHY for transient early warnings.
            if (previousHealth != HealthStatus.UNHEALTHY && incidentResult.isEmpty()) {
                notificationService.notifyServiceUnhealthy(monitor, failureReason, savedResult.getId());
            }
        }

        return savedResult;
    }
}
