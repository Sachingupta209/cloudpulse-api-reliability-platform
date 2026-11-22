package com.cloudpulse.analytics;

import com.cloudpulse.anomaly.Anomaly;
import com.cloudpulse.anomaly.AnomalyRepository;
import com.cloudpulse.anomaly.AnomalySeverity;
import com.cloudpulse.anomaly.AnomalyType;
import com.cloudpulse.incident.Incident;
import com.cloudpulse.incident.IncidentRepository;
import com.cloudpulse.incident.IncidentStatus;
import com.cloudpulse.monitor.HealthStatus;
import com.cloudpulse.monitor.HttpMethod;
import com.cloudpulse.monitor.Monitor;
import com.cloudpulse.monitor.MonitorRepository;
import com.cloudpulse.result.CheckResult;
import com.cloudpulse.result.CheckResultRepository;
import com.cloudpulse.result.CheckStatus;
import com.cloudpulse.security.UserPrincipal;
import com.cloudpulse.user.Role;
import com.cloudpulse.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private MonitorRepository monitorRepository;
    @Mock
    private CheckResultRepository checkResultRepository;
    @Mock
    private IncidentRepository incidentRepository;
    @Mock
    private AnomalyRepository anomalyRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private User testUser;
    private Monitor testMonitor;

    @BeforeEach
    void setUp() {
        testUser = new User("analyst@cloudpulse.io", "pass", "Jane", "Analyst", Role.ROLE_USER);
        ReflectionTestUtils.setField(testUser, "id", 1L);

        testMonitor = new Monitor(testUser, "Payments Gateway", "https://api.payments.io", HttpMethod.GET, 60, 5000, 200);
        ReflectionTestUtils.setField(testMonitor, "id", 10L);

        UserPrincipal principal = UserPrincipal.create(testUser);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getDashboardSummary_Success() {
        when(monitorRepository.countByUserId(1L)).thenReturn(2L);
        when(monitorRepository.countByUserIdAndHealthStatus(1L, HealthStatus.HEALTHY)).thenReturn(2L);
        when(monitorRepository.countByUserIdAndHealthStatus(1L, HealthStatus.UNHEALTHY)).thenReturn(0L);
        when(monitorRepository.countByUserIdAndStatus(1L, com.cloudpulse.monitor.MonitorStatus.PAUSED)).thenReturn(0L);

        when(checkResultRepository.countByUserId(1L)).thenReturn(100L);
        when(checkResultRepository.countFailuresByUserId(1L)).thenReturn(5L);
        when(checkResultRepository.findAverageResponseTimeByUserId(1L)).thenReturn(45.5);
        when(incidentRepository.countByUserIdAndStatus(1L, IncidentStatus.OPEN)).thenReturn(0L);
        when(incidentRepository.findTop5ByUserIdOrderByStartedAtDesc(1L)).thenReturn(List.of());
        when(monitorRepository.findByUserId(1L)).thenReturn(List.of(testMonitor));

        DashboardSummaryDto summary = analyticsService.getDashboardSummary();

        assertNotNull(summary);
        assertEquals(2L, summary.getTotalMonitors());
        assertEquals(95.0, summary.getOverallUptimePercentage());
        assertEquals(45.5, summary.getAverageResponseTimeMs());
        assertEquals(100L, summary.getTotalChecks());
        assertEquals(5L, summary.getFailedChecks());
    }

    @Test
    void getAdvancedAnalytics_Success_CalculatesMTTR_MTBF_Percentiles() {
        when(monitorRepository.findByUserId(1L)).thenReturn(List.of(testMonitor));
        when(checkResultRepository.countByUserId(1L)).thenReturn(100L);
        when(checkResultRepository.countFailuresByUserId(1L)).thenReturn(2L);

        // Incident with 20 minutes duration
        Instant started = Instant.now().minus(2, ChronoUnit.HOURS);
        Instant resolved = started.plus(20, ChronoUnit.MINUTES);
        Incident incident = new Incident(testMonitor, testUser, "Gateway 502", "Upstream timeout", started);
        incident.resolve(resolved);

        when(incidentRepository.findByUserIdAndStatus(1L, IncidentStatus.RESOLVED)).thenReturn(List.of(incident));
        when(incidentRepository.countByUserId(1L)).thenReturn(1L);

        // Latency samples
        List<CheckResult> checks = new ArrayList<>();
        long[] lats = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
        for (long l : lats) {
            checks.add(new CheckResult(testMonitor, CheckStatus.SUCCESS, 200, l, null, null, "127.0.0.1"));
        }
        when(checkResultRepository.findRecentByMonitorId(eq(10L), any())).thenReturn(checks);

        // Anomalies
        Anomaly anomaly = new Anomaly(testMonitor, testUser, AnomalyType.LATENCY_SPIKE, AnomalySeverity.HIGH, 300.0, 50.0, "Spike", Instant.now());
        when(anomalyRepository.findByUserIdWithMonitor(eq(1L), any())).thenReturn(new PageImpl<>(List.of(anomaly)));
        when(anomalyRepository.countByUserIdAndAcknowledgedFalse(1L)).thenReturn(1L);

        AdvancedAnalyticsDto dto = analyticsService.getAdvancedAnalytics();

        assertNotNull(dto);
        assertEquals(98.0, dto.overallUptimePercentage());
        assertEquals(2.0, dto.failureRatePercentage());
        assertEquals(20.0, dto.mttrMinutes()); // 20 minutes
        assertNotNull(dto.mtbfHours());
        assertEquals(10L, dto.minLatencyMs());
        assertEquals(100L, dto.maxLatencyMs());
        assertEquals(100L, dto.p95LatencyMs());
        assertEquals(1, dto.recentAnomalies().size());
        assertEquals(1L, dto.unacknowledgedAnomalies());
    }
}
