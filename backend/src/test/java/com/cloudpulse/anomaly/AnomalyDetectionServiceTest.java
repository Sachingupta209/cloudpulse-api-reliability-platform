package com.cloudpulse.anomaly;

import com.cloudpulse.exception.ResourceNotFoundException;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnomalyDetectionServiceTest {

    @Mock
    private AnomalyRepository anomalyRepository;
    @Mock
    private CheckResultRepository checkResultRepository;
    @Mock
    private MonitorRepository monitorRepository;

    @InjectMocks
    private AnomalyDetectionService anomalyDetectionService;

    private User testUser;
    private Monitor testMonitor;

    @BeforeEach
    void setUp() {
        testUser = new User("user@cloudpulse.io", "hashedPass", "Jane", "User", Role.ROLE_USER);
        ReflectionTestUtils.setField(testUser, "id", 1L);

        testMonitor = new Monitor(testUser, "Auth Service", "https://auth.cloudpulse.io/health", HttpMethod.GET, 60, 5000, 200);
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
    void detectLatencySpike_SavesAnomalyWhenZScoreExceedsThreshold() {
        // Create baseline checks with low latency (around 50ms)
        List<CheckResult> baseline = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            CheckResult cr = new CheckResult(testMonitor, CheckStatus.SUCCESS, 200, 50L + (i % 5), null, null, "127.0.0.1");
            ReflectionTestUtils.setField(cr, "id", (long) i + 1);
            baseline.add(cr);
        }

        // Current check result with high latency spike (800ms)
        CheckResult current = new CheckResult(testMonitor, CheckStatus.SUCCESS, 200, 800L, null, null, "127.0.0.1");
        ReflectionTestUtils.setField(current, "id", 999L);

        when(checkResultRepository.findRecentByMonitorId(eq(10L), any())).thenReturn(baseline);
        when(anomalyRepository.existsRecentByMonitorAndType(eq(10L), eq(AnomalyType.LATENCY_SPIKE), any()))
                .thenReturn(false);

        anomalyDetectionService.analyzeCheckResult(testMonitor, current);

        ArgumentCaptor<Anomaly> captor = ArgumentCaptor.forClass(Anomaly.class);
        verify(anomalyRepository, atLeastOnce()).save(captor.capture());

        Anomaly saved = captor.getValue();
        assertEquals(AnomalyType.LATENCY_SPIKE, saved.getAnomalyType());
        assertTrue(saved.getObservedValue() >= 800.0);
        assertTrue(saved.getSeverity() == AnomalySeverity.HIGH || saved.getSeverity() == AnomalySeverity.CRITICAL);
        assertFalse(saved.isAcknowledged());
    }

    @Test
    void detectLatencySpike_SkipsWhenUnderBaselineOrLowLatency() {
        CheckResult current = new CheckResult(testMonitor, CheckStatus.SUCCESS, 200, 45L, null, null, "127.0.0.1");
        ReflectionTestUtils.setField(current, "id", 999L);

        anomalyDetectionService.analyzeCheckResult(testMonitor, current);

        verify(anomalyRepository, never()).save(any());
    }

    @Test
    void detectLatencySpike_RespectsCoolingOffWindow() {
        List<CheckResult> baseline = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            CheckResult cr = new CheckResult(testMonitor, CheckStatus.SUCCESS, 200, 50L, null, null, "127.0.0.1");
            ReflectionTestUtils.setField(cr, "id", (long) i + 1);
            baseline.add(cr);
        }

        CheckResult current = new CheckResult(testMonitor, CheckStatus.SUCCESS, 200, 800L, null, null, "127.0.0.1");
        ReflectionTestUtils.setField(current, "id", 999L);

        when(checkResultRepository.findRecentByMonitorId(eq(10L), any())).thenReturn(baseline);
        when(anomalyRepository.existsRecentByMonitorAndType(eq(10L), eq(AnomalyType.LATENCY_SPIKE), any()))
                .thenReturn(true); // Cooling off active

        anomalyDetectionService.analyzeCheckResult(testMonitor, current);

        verify(anomalyRepository, never()).save(argThat(a -> a.getAnomalyType() == AnomalyType.LATENCY_SPIKE));
    }

    @Test
    void detectFailureBurst_SavesAnomalyWhenConsecutiveFailuresThresholdMet() {
        testMonitor.setConsecutiveFailures(3);
        CheckResult current = new CheckResult(testMonitor, CheckStatus.FAILURE, 500, 100L, "Timeout", null, "127.0.0.1");
        ReflectionTestUtils.setField(current, "id", 999L);

        when(anomalyRepository.existsRecentByMonitorAndType(eq(10L), eq(AnomalyType.FAILURE_BURST), any()))
                .thenReturn(false);

        anomalyDetectionService.analyzeCheckResult(testMonitor, current);

        ArgumentCaptor<Anomaly> captor = ArgumentCaptor.forClass(Anomaly.class);
        verify(anomalyRepository, atLeastOnce()).save(captor.capture());

        Anomaly saved = captor.getValue();
        assertEquals(AnomalyType.FAILURE_BURST, saved.getAnomalyType());
        assertEquals(3.0, saved.getObservedValue());
        assertEquals(AnomalySeverity.HIGH, saved.getSeverity());
    }

    @Test
    void acknowledgeAnomaly_Success() {
        Anomaly anomaly = new Anomaly(testMonitor, testUser, AnomalyType.LATENCY_SPIKE, AnomalySeverity.HIGH, 750.0, 50.0, "Spike", Instant.now());
        ReflectionTestUtils.setField(anomaly, "id", 100L);

        when(anomalyRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(anomaly));
        when(anomalyRepository.save(any(Anomaly.class))).thenAnswer(inv -> inv.getArgument(0));

        AnomalyDto dto = anomalyDetectionService.acknowledgeAnomaly(100L);

        assertTrue(dto.acknowledged());
        assertNotNull(dto.acknowledgedAt());
        verify(anomalyRepository).save(anomaly);
    }

    @Test
    void acknowledgeAnomaly_NotFound_ThrowsException() {
        when(anomalyRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                anomalyDetectionService.acknowledgeAnomaly(999L));
    }

    @Test
    void getUnacknowledgedCount_ReturnsCount() {
        when(anomalyRepository.countByUserIdAndAcknowledgedFalse(1L)).thenReturn(5L);

        long count = anomalyDetectionService.getUnacknowledgedCount();

        assertEquals(5L, count);
    }
}
