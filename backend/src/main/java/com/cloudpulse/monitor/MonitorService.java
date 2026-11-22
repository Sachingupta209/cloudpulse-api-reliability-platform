package com.cloudpulse.monitor;

import com.cloudpulse.exception.BadRequestException;
import com.cloudpulse.exception.ResourceNotFoundException;
import com.cloudpulse.monitoring.MonitoringService;
import com.cloudpulse.monitoring.SsrfProtectionService;
import com.cloudpulse.result.CheckResult;
import com.cloudpulse.result.CheckResultDto;
import com.cloudpulse.result.CheckResultRepository;
import com.cloudpulse.security.SecurityUtils;
import com.cloudpulse.user.User;
import com.cloudpulse.user.UserRepository;
import com.cloudpulse.validation.ValidationRule;
import com.cloudpulse.validation.ValidationRuleDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class MonitorService {

    private final MonitorRepository monitorRepository;
    private final UserRepository userRepository;
    private final CheckResultRepository checkResultRepository;
    private final SsrfProtectionService ssrfProtectionService;
    private final MonitoringService monitoringService;

    public MonitorService(
            MonitorRepository monitorRepository,
            UserRepository userRepository,
            CheckResultRepository checkResultRepository,
            SsrfProtectionService ssrfProtectionService,
            MonitoringService monitoringService) {
        this.monitorRepository = monitorRepository;
        this.userRepository = userRepository;
        this.checkResultRepository = checkResultRepository;
        this.ssrfProtectionService = ssrfProtectionService;
        this.monitoringService = monitoringService;
    }

    @Transactional
    public MonitorResponse createMonitor(MonitorRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Validate URL against SSRF policy upfront
        ssrfProtectionService.validateUri(request.getUrl());

        Monitor monitor = new Monitor(
                user,
                request.getName(),
                request.getUrl(),
                request.getHttpMethod(),
                request.getCheckIntervalSeconds(),
                request.getTimeoutMs(),
                request.getExpectedStatusCode()
        );
        monitor.setHeadersJson(request.getHeadersJson());
        monitor.setRequestBody(request.getRequestBody());

        if (request.getValidationRules() != null) {
            for (ValidationRuleDto ruleDto : request.getValidationRules()) {
                ValidationRule rule = new ValidationRule(
                        monitor,
                        ruleDto.getRuleType(),
                        ruleDto.getTargetExpression(),
                        ruleDto.getExpectedValue(),
                        ruleDto.getErrorMessage(),
                        ruleDto.isEnabled()
                );
                monitor.addValidationRule(rule);
            }
        }

        monitor = monitorRepository.save(monitor);
        return MonitorResponse.from(monitor);
    }

    @Transactional(readOnly = true)
    public List<MonitorResponse> getUserMonitors() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Monitor> monitors = monitorRepository.findByUserId(userId);
        Instant last24Hours = Instant.now().minus(24, ChronoUnit.HOURS);

        return monitors.stream().map(monitor -> {
            Double avgLatency = checkResultRepository.findAverageResponseTimeByMonitorIdSince(monitor.getId(), last24Hours);
            long total = checkResultRepository.countByMonitorIdSince(monitor.getId(), last24Hours);
            long successful = checkResultRepository.countSuccessfulByMonitorIdSince(monitor.getId(), last24Hours);
            Double uptime = (total > 0) ? ((double) successful / total) * 100.0 : 100.0;

            return MonitorResponse.from(monitor, uptime, avgLatency);
        }).toList();
    }

    @Transactional(readOnly = true)
    public MonitorResponse getMonitorById(Long id) {
        Monitor monitor = getMonitorEntityForCurrentUser(id);
        Instant last24Hours = Instant.now().minus(24, ChronoUnit.HOURS);

        Double avgLatency = checkResultRepository.findAverageResponseTimeByMonitorIdSince(monitor.getId(), last24Hours);
        long total = checkResultRepository.countByMonitorIdSince(monitor.getId(), last24Hours);
        long successful = checkResultRepository.countSuccessfulByMonitorIdSince(monitor.getId(), last24Hours);
        Double uptime = (total > 0) ? ((double) successful / total) * 100.0 : 100.0;

        return MonitorResponse.from(monitor, uptime, avgLatency);
    }

    @Transactional
    public MonitorResponse updateMonitor(Long id, MonitorRequest request) {
        Monitor monitor = getMonitorEntityForCurrentUser(id);

        // Validate URL if modified
        if (!monitor.getUrl().equals(request.getUrl())) {
            ssrfProtectionService.validateUri(request.getUrl());
            monitor.setUrl(request.getUrl());
        }

        monitor.setName(request.getName());
        monitor.setHttpMethod(request.getHttpMethod());
        monitor.setCheckIntervalSeconds(request.getCheckIntervalSeconds());
        monitor.setTimeoutMs(request.getTimeoutMs());
        monitor.setExpectedStatusCode(request.getExpectedStatusCode());
        monitor.setHeadersJson(request.getHeadersJson());
        monitor.setRequestBody(request.getRequestBody());

        // Update validation rules
        monitor.clearValidationRules();
        if (request.getValidationRules() != null) {
            for (ValidationRuleDto ruleDto : request.getValidationRules()) {
                ValidationRule rule = new ValidationRule(
                        monitor,
                        ruleDto.getRuleType(),
                        ruleDto.getTargetExpression(),
                        ruleDto.getExpectedValue(),
                        ruleDto.getErrorMessage(),
                        ruleDto.isEnabled()
                );
                monitor.addValidationRule(rule);
            }
        }

        monitor = monitorRepository.save(monitor);
        return MonitorResponse.from(monitor);
    }

    @Transactional
    public void deleteMonitor(Long id) {
        Monitor monitor = getMonitorEntityForCurrentUser(id);
        monitorRepository.delete(monitor);
    }

    @Transactional
    public MonitorResponse pauseMonitor(Long id) {
        Monitor monitor = getMonitorEntityForCurrentUser(id);
        monitor.setStatus(MonitorStatus.PAUSED);
        monitor = monitorRepository.save(monitor);
        return MonitorResponse.from(monitor);
    }

    @Transactional
    public MonitorResponse resumeMonitor(Long id) {
        Monitor monitor = getMonitorEntityForCurrentUser(id);
        monitor.setStatus(MonitorStatus.ACTIVE);
        monitor = monitorRepository.save(monitor);
        return MonitorResponse.from(monitor);
    }

    @Transactional
    public CheckResultDto triggerManualCheck(Long id) {
        Monitor monitor = getMonitorEntityForCurrentUser(id);
        CheckResult result = monitoringService.executeCheck(monitor);
        return CheckResultDto.from(result);
    }

    public Monitor getMonitorEntityForCurrentUser(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return monitorRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor", "id", id));
    }
}
