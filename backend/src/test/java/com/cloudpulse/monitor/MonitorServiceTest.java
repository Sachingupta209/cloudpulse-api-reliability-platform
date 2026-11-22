package com.cloudpulse.monitor;

import com.cloudpulse.exception.ResourceNotFoundException;
import com.cloudpulse.exception.SsrfViolationException;
import com.cloudpulse.monitoring.MonitoringService;
import com.cloudpulse.monitoring.SsrfProtectionService;
import com.cloudpulse.result.CheckResultRepository;
import com.cloudpulse.security.UserPrincipal;
import com.cloudpulse.user.Role;
import com.cloudpulse.user.User;
import com.cloudpulse.user.UserRepository;
import com.cloudpulse.validation.RuleType;
import com.cloudpulse.validation.ValidationRuleDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitorServiceTest {

    @Mock
    private MonitorRepository monitorRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CheckResultRepository checkResultRepository;
    @Mock
    private SsrfProtectionService ssrfProtectionService;
    @Mock
    private MonitoringService monitoringService;

    private MonitorService monitorService;
    private User testUser;

    @BeforeEach
    void setUp() {
        monitorService = new MonitorService(
                monitorRepository,
                userRepository,
                checkResultRepository,
                ssrfProtectionService,
                monitoringService
        );

        testUser = new User("alice@cloudpulse.io", "hash", "Alice", "Smith", Role.ROLE_USER);
        testUser.setId(100L);

        UserPrincipal principal = UserPrincipal.create(testUser);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should create monitor when valid URL is supplied")
    void shouldCreateMonitor() {
        MonitorRequest req = new MonitorRequest("Production API", "https://api.example.com", HttpMethod.GET, 60, 5000, 200);

        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));
        when(monitorRepository.save(any(Monitor.class))).thenAnswer(inv -> {
            Monitor m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });

        MonitorResponse response = monitorService.createMonitor(req);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Production API");
        verify(ssrfProtectionService).validateUri("https://api.example.com");
        verify(monitorRepository).save(any(Monitor.class));
    }

    @Test
    @DisplayName("Should reject monitor creation if URL violates SSRF policy")
    void shouldRejectMonitorCreationOnSsrfViolation() {
        MonitorRequest req = new MonitorRequest("Internal DB", "http://169.254.169.254/latest/meta-data/", HttpMethod.GET, 60, 5000, 200);

        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));
        doThrow(new SsrfViolationException("Access to cloud metadata IP address is forbidden"))
                .when(ssrfProtectionService).validateUri(req.getUrl());

        assertThatThrownBy(() -> monitorService.createMonitor(req))
                .isInstanceOf(SsrfViolationException.class)
                .hasMessageContaining("cloud metadata");

        verify(monitorRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should enforce user isolation: access denied to another user's monitor")
    void shouldEnforceUserIsolation() {
        // Monitor belongs to another user (not 100L)
        when(monitorRepository.findByIdAndUserId(999L, 100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> monitorService.getMonitorById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should pause and resume monitor")
    void shouldPauseAndResumeMonitor() {
        Monitor monitor = new Monitor(testUser, "Test API", "https://api.example.com", HttpMethod.GET, 60, 5000, 200);
        monitor.setId(5L);

        when(monitorRepository.findByIdAndUserId(5L, 100L)).thenReturn(Optional.of(monitor));
        when(monitorRepository.save(any(Monitor.class))).thenAnswer(inv -> inv.getArgument(0));

        MonitorResponse paused = monitorService.pauseMonitor(5L);
        assertThat(paused.getStatus()).isEqualTo(MonitorStatus.PAUSED);

        MonitorResponse resumed = monitorService.resumeMonitor(5L);
        assertThat(resumed.getStatus()).isEqualTo(MonitorStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should update existing monitor in-place without creating a new entity or deleting history")
    void shouldUpdateExistingMonitorWithoutRecreating() {
        Monitor existing = new Monitor(testUser, "Old Service", "https://api.example.com/v1", HttpMethod.GET, 60, 5000, 200);
        existing.setId(42L);

        MonitorRequest updateReq = new MonitorRequest("New Service Name", "https://api.example.com/v2", HttpMethod.POST, 120, 8000, 201);
        updateReq.setRequestBody("{\"query\": \"ping\"}");
        ValidationRuleDto rule = new ValidationRuleDto(null, RuleType.JSON_PATH_EQUALS, "$.status", "ok", "Status must be ok", true);
        updateReq.setValidationRules(List.of(rule));

        when(monitorRepository.findByIdAndUserId(42L, 100L)).thenReturn(Optional.of(existing));
        when(monitorRepository.save(any(Monitor.class))).thenAnswer(inv -> inv.getArgument(0));

        MonitorResponse response = monitorService.updateMonitor(42L, updateReq);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(42L);
        assertThat(response.getName()).isEqualTo("New Service Name");
        assertThat(response.getUrl()).isEqualTo("https://api.example.com/v2");
        assertThat(response.getHttpMethod()).isEqualTo(HttpMethod.POST);
        assertThat(response.getCheckIntervalSeconds()).isEqualTo(120);
        assertThat(response.getTimeoutMs()).isEqualTo(8000);
        assertThat(response.getExpectedStatusCode()).isEqualTo(201);
        assertThat(response.getValidationRules()).hasSize(1);
        assertThat(response.getValidationRules().get(0).getTargetExpression()).isEqualTo("$.status");

        // Verify URL re-validation occurred because URL was changed
        verify(ssrfProtectionService).validateUri("https://api.example.com/v2");
        verify(monitorRepository).save(existing);
        verify(monitorRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should update monitor without triggering SSRF validation if URL remains identical")
    void shouldNotRevalidateUrlIfUnchanged() {
        Monitor existing = new Monitor(testUser, "Old Service", "https://api.example.com/health", HttpMethod.GET, 60, 5000, 200);
        existing.setId(42L);

        MonitorRequest updateReq = new MonitorRequest("Updated Name", "https://api.example.com/health", HttpMethod.GET, 30, 3000, 200);

        when(monitorRepository.findByIdAndUserId(42L, 100L)).thenReturn(Optional.of(existing));
        when(monitorRepository.save(any(Monitor.class))).thenAnswer(inv -> inv.getArgument(0));

        MonitorResponse response = monitorService.updateMonitor(42L, updateReq);

        assertThat(response.getId()).isEqualTo(42L);
        assertThat(response.getName()).isEqualTo("Updated Name");
        verify(ssrfProtectionService, never()).validateUri(any());
        verify(monitorRepository).save(existing);
    }

    @Test
    @DisplayName("Should reject monitor update if changed URL violates SSRF policy")
    void shouldRejectMonitorUpdateOnSsrfViolation() {
        Monitor existing = new Monitor(testUser, "Valid Service", "https://api.example.com/health", HttpMethod.GET, 60, 5000, 200);
        existing.setId(42L);

        MonitorRequest updateReq = new MonitorRequest("Malicious Update", "http://192.168.1.100:8080/metrics", HttpMethod.GET, 60, 5000, 200);

        when(monitorRepository.findByIdAndUserId(42L, 100L)).thenReturn(Optional.of(existing));
        doThrow(new SsrfViolationException("Access to private IPv4 address range is forbidden"))
                .when(ssrfProtectionService).validateUri("http://192.168.1.100:8080/metrics");

        assertThatThrownBy(() -> monitorService.updateMonitor(42L, updateReq))
                .isInstanceOf(SsrfViolationException.class)
                .hasMessageContaining("private IPv4");

        verify(monitorRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject monitor update if monitor does not belong to current user")
    void shouldRejectUpdateWhenNotOwner() {
        when(monitorRepository.findByIdAndUserId(999L, 100L)).thenReturn(Optional.empty());

        MonitorRequest updateReq = new MonitorRequest("Hijack Attempt", "https://api.example.com", HttpMethod.GET, 60, 5000, 200);

        assertThatThrownBy(() -> monitorService.updateMonitor(999L, updateReq))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Monitor not found with id : '999'");

        verify(monitorRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return monitor details with initialized validation rules, uptime and latency")
    void shouldReturnMonitorDetailsWhenOwner() {
        Monitor monitor = new Monitor(testUser, "Payments Service", "https://api.example.com/pay", HttpMethod.POST, 60, 5000, 200);
        monitor.setId(50L);
        ValidationRuleDto rule = new ValidationRuleDto(null, RuleType.RESPONSE_TIME, null, "300", "Latency exceeded", true);
        monitor.addValidationRule(new com.cloudpulse.validation.ValidationRule(monitor, RuleType.RESPONSE_TIME, null, "300", "Latency exceeded", true));

        when(monitorRepository.findByIdAndUserId(50L, 100L)).thenReturn(Optional.of(monitor));
        when(checkResultRepository.findAverageResponseTimeByMonitorIdSince(eq(50L), any(java.time.Instant.class))).thenReturn(145.5);
        when(checkResultRepository.countByMonitorIdSince(eq(50L), any(java.time.Instant.class))).thenReturn(100L);
        when(checkResultRepository.countSuccessfulByMonitorIdSince(eq(50L), any(java.time.Instant.class))).thenReturn(99L);

        MonitorResponse response = monitorService.getMonitorById(50L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(50L);
        assertThat(response.getName()).isEqualTo("Payments Service");
        assertThat(response.getValidationRules()).hasSize(1);
        assertThat(response.getUptimePercentage()).isEqualTo(99.0);
        assertThat(response.getAvgResponseTimeMs()).isEqualTo(145.5);
    }

    @Test
    @DisplayName("Should return all monitors for current user with uptime and latency statistics")
    void shouldReturnAllUserMonitors() {
        Monitor m1 = new Monitor(testUser, "Service A", "https://api.example.com/a", HttpMethod.GET, 60, 5000, 200);
        m1.setId(1L);
        Monitor m2 = new Monitor(testUser, "Service B", "https://api.example.com/b", HttpMethod.GET, 60, 5000, 200);
        m2.setId(2L);

        when(monitorRepository.findByUserId(100L)).thenReturn(List.of(m1, m2));
        when(checkResultRepository.countByMonitorIdSince(anyLong(), any(java.time.Instant.class))).thenReturn(10L);
        when(checkResultRepository.countSuccessfulByMonitorIdSince(anyLong(), any(java.time.Instant.class))).thenReturn(10L);
        when(checkResultRepository.findAverageResponseTimeByMonitorIdSince(anyLong(), any(java.time.Instant.class))).thenReturn(80.0);

        List<MonitorResponse> responses = monitorService.getUserMonitors();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getName()).isEqualTo("Service A");
        assertThat(responses.get(0).getUptimePercentage()).isEqualTo(100.0);
        assertThat(responses.get(1).getName()).isEqualTo("Service B");
    }

    @Test
    @DisplayName("Tenant Isolation: Should reject delete when user is not owner")
    void shouldRejectDeleteWhenNotOwner() {
        when(monitorRepository.findByIdAndUserId(999L, 100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> monitorService.deleteMonitor(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Monitor not found with id : '999'");

        verify(monitorRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Tenant Isolation: Should reject trigger manual check when user is not owner")
    void shouldRejectManualCheckWhenNotOwner() {
        when(monitorRepository.findByIdAndUserId(999L, 100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> monitorService.triggerManualCheck(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Monitor not found with id : '999'");

        verify(monitoringService, never()).executeCheck(any());
    }
}
