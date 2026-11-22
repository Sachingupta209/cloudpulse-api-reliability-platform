package com.cloudpulse.monitoring;

import com.cloudpulse.incident.IncidentService;
import com.cloudpulse.monitor.*;
import com.cloudpulse.result.CheckResult;
import com.cloudpulse.result.CheckResultRepository;
import com.cloudpulse.result.CheckStatus;
import com.cloudpulse.user.Role;
import com.cloudpulse.user.User;
import com.cloudpulse.validation.RuleType;
import com.cloudpulse.validation.ValidationEngine;
import com.cloudpulse.validation.ValidationResult;
import com.cloudpulse.validation.ValidationRule;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitoringServiceTest {

    @Mock
    private MonitoringHttpClient httpClient;
    @Mock
    private ValidationEngine validationEngine;
    @Mock
    private CheckResultRepository checkResultRepository;
    @Mock
    private MonitorRepository monitorRepository;
    @Mock
    private IncidentService incidentService;
    @Mock
    private com.cloudpulse.notification.NotificationService notificationService;

    private MonitoringService monitoringService;
    private ObjectMapper objectMapper;
    private User testUser;
    private Monitor testMonitor;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        monitoringService = new MonitoringService(
                httpClient,
                validationEngine,
                checkResultRepository,
                monitorRepository,
                incidentService,
                notificationService,
                objectMapper,
                3
        );

        testUser = new User("user@cloudpulse.io", "hash", "Bob", "Builder", Role.ROLE_USER);
        testUser.setId(10L);

        testMonitor = new Monitor(testUser, "Payments API", "https://api.example.com/pay", HttpMethod.POST, 60, 5000, 200);
        testMonitor.setId(100L);
    }

    @Test
    @DisplayName("Scheduled check with validation rules: evaluates rules and marks healthy on pass")
    void shouldExecuteCheckWithValidationRules_Passing() {
        ValidationRule rule = new ValidationRule(
                testMonitor,
                RuleType.JSON_PATH_EQUALS,
                "$.status",
                "SUCCESS",
                "Status must be SUCCESS",
                true
        );
        testMonitor.addValidationRule(rule);

        when(monitorRepository.findByIdWithValidationRules(100L)).thenReturn(Optional.of(testMonitor));

        HttpResponseSnapshot snapshot = new HttpResponseSnapshot(
                true,
                200,
                120L,
                Map.of("content-type", List.of("application/json")),
                "{\"status\": \"SUCCESS\"}",
                "104.21.5.12",
                null
        );
        when(httpClient.execute(eq("https://api.example.com/pay"), eq(HttpMethod.POST), anyInt(), any(), any()))
                .thenReturn(snapshot);

        ValidationResult vr = ValidationResult.success("JSON_PATH_EQUALS", "$.status", "SUCCESS");
        when(validationEngine.validate(eq(200), eq(120L), any(), eq("{\"status\": \"SUCCESS\"}"), anyList()))
                .thenReturn(List.of(vr));

        when(monitorRepository.save(any(Monitor.class))).thenAnswer(inv -> inv.getArgument(0));
        when(checkResultRepository.save(any(CheckResult.class))).thenAnswer(inv -> inv.getArgument(0));

        CheckResult result = monitoringService.executeCheckById(100L);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(CheckStatus.SUCCESS);
        assertThat(result.getHttpStatusCode()).isEqualTo(200);
        assertThat(result.getResponseTimeMs()).isEqualTo(120L);
        assertThat(testMonitor.getHealthStatus()).isEqualTo(HealthStatus.HEALTHY);
        assertThat(testMonitor.getConsecutiveFailures()).isZero();

        verify(incidentService).handleRecovery(testMonitor);
        verify(incidentService, never()).handleFailure(any(), any(), anyInt(), anyInt());
        verify(monitorRepository).save(testMonitor);
        verify(checkResultRepository).save(any(CheckResult.class));
    }

    @Test
    @DisplayName("Scheduled check without validation rules: validates HTTP status code directly")
    void shouldExecuteCheckWithoutValidationRules_Passing() {
        when(monitorRepository.findByIdWithValidationRules(100L)).thenReturn(Optional.of(testMonitor));

        HttpResponseSnapshot snapshot = new HttpResponseSnapshot(
                true,
                200,
                85L,
                Map.of(),
                "OK",
                "104.21.5.12",
                null
        );
        when(httpClient.execute(anyString(), any(), anyInt(), any(), any()))
                .thenReturn(snapshot);
        when(validationEngine.validate(eq(200), eq(85L), any(), eq("OK"), anyList()))
                .thenReturn(List.of());
        when(monitorRepository.save(any(Monitor.class))).thenAnswer(inv -> inv.getArgument(0));
        when(checkResultRepository.save(any(CheckResult.class))).thenAnswer(inv -> inv.getArgument(0));

        CheckResult result = monitoringService.executeCheckById(100L);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(CheckStatus.SUCCESS);
        assertThat(testMonitor.getHealthStatus()).isEqualTo(HealthStatus.HEALTHY);
        verify(incidentService).handleRecovery(testMonitor);
    }

    @Test
    @DisplayName("Scheduled check with failing validation rule: marks monitor unhealthy and triggers incident handling")
    void shouldExecuteCheckWithValidationRules_Failing() {
        ValidationRule rule = new ValidationRule(
                testMonitor,
                RuleType.JSON_PATH_EQUALS,
                "$.status",
                "SUCCESS",
                "Status must be SUCCESS",
                true
        );
        testMonitor.addValidationRule(rule);

        when(monitorRepository.findByIdWithValidationRules(100L)).thenReturn(Optional.of(testMonitor));

        HttpResponseSnapshot snapshot = new HttpResponseSnapshot(
                true,
                200,
                140L,
                Map.of(),
                "{\"status\": \"ERROR\", \"message\": \"Database timeout\"}",
                "104.21.5.12",
                null
        );
        when(httpClient.execute(anyString(), any(), anyInt(), any(), any())).thenReturn(snapshot);

        ValidationResult vr = ValidationResult.failure("JSON_PATH_EQUALS", "$.status", "SUCCESS", "ERROR", "Status must be SUCCESS");
        when(validationEngine.validate(anyInt(), anyLong(), any(), anyString(), anyList()))
                .thenReturn(List.of(vr));
        when(monitorRepository.save(any(Monitor.class))).thenAnswer(inv -> inv.getArgument(0));
        when(checkResultRepository.save(any(CheckResult.class))).thenAnswer(inv -> inv.getArgument(0));

        CheckResult result = monitoringService.executeCheckById(100L);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(CheckStatus.FAILURE);
        assertThat(result.getFailureReason()).contains("Status must be SUCCESS");
        assertThat(testMonitor.getHealthStatus()).isEqualTo(HealthStatus.UNHEALTHY);
        assertThat(testMonitor.getConsecutiveFailures()).isEqualTo(1);

        verify(incidentService).handleFailure(eq(testMonitor), contains("Status must be SUCCESS"), eq(1), eq(3));
    }

    @Test
    @DisplayName("Scheduled check should skip inactive / paused monitors")
    void shouldSkipInactiveMonitors() {
        testMonitor.setStatus(MonitorStatus.PAUSED);
        when(monitorRepository.findByIdWithValidationRules(100L)).thenReturn(Optional.of(testMonitor));

        CheckResult result = monitoringService.executeCheckById(100L);

        assertThat(result).isNull();
        verify(httpClient, never()).execute(any(), any(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("Scheduled check should return null if monitor not found in database")
    void shouldReturnNullWhenMonitorNotFound() {
        when(monitorRepository.findByIdWithValidationRules(999L)).thenReturn(Optional.empty());

        CheckResult result = monitoringService.executeCheckById(999L);

        assertThat(result).isNull();
        verify(httpClient, never()).execute(any(), any(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("executeCheck() re-fetches entity via findByIdWithValidationRules to guarantee initialized associations")
    void shouldReFetchEntityToAvoidLazyInitializationException() {
        // Monitor passed as argument (e.g. from scheduler or manual check trigger)
        Monitor inputMonitor = new Monitor(testUser, "Input", "https://api.example.com", HttpMethod.GET, 60, 5000, 200);
        inputMonitor.setId(100L);

        // Attached monitor with loaded validation rules returned by JOIN FETCH
        Monitor attachedMonitor = new Monitor(testUser, "Attached", "https://api.example.com", HttpMethod.GET, 60, 5000, 200);
        attachedMonitor.setId(100L);
        attachedMonitor.addValidationRule(new ValidationRule(attachedMonitor, RuleType.RESPONSE_TIME, null, "500", "Latency > 500ms", true));

        when(monitorRepository.findByIdWithValidationRules(100L)).thenReturn(Optional.of(attachedMonitor));

        HttpResponseSnapshot snapshot = new HttpResponseSnapshot(true, 200, 110L, Map.of(), "ok", "1.1.1.1", null);
        when(httpClient.execute(anyString(), any(), anyInt(), any(), any())).thenReturn(snapshot);
        when(validationEngine.validate(anyInt(), anyLong(), any(), anyString(), anyList())).thenReturn(List.of());
        when(monitorRepository.save(any(Monitor.class))).thenAnswer(inv -> inv.getArgument(0));
        when(checkResultRepository.save(any(CheckResult.class))).thenAnswer(inv -> inv.getArgument(0));

        CheckResult result = monitoringService.executeCheck(inputMonitor);

        assertThat(result).isNotNull();
        // Verify that findByIdWithValidationRules was invoked with inputMonitor.getId()
        verify(monitorRepository).findByIdWithValidationRules(100L);
        // Verify that the attached monitor was saved
        verify(monitorRepository).save(attachedMonitor);
    }
}
