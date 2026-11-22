package com.cloudpulse.report;

import com.cloudpulse.exception.BadRequestException;
import com.cloudpulse.exception.ResourceNotFoundException;
import com.cloudpulse.incident.Incident;
import com.cloudpulse.incident.IncidentRepository;
import com.cloudpulse.monitor.HttpMethod;
import com.cloudpulse.monitor.Monitor;
import com.cloudpulse.monitor.MonitorRepository;
import com.cloudpulse.result.CheckResult;
import com.cloudpulse.result.CheckResultRepository;
import com.cloudpulse.result.CheckStatus;
import com.cloudpulse.security.UserPrincipal;
import com.cloudpulse.user.Role;
import com.cloudpulse.user.User;
import com.cloudpulse.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
class ReportServiceTest {

    @Mock
    private MonitorRepository monitorRepository;
    @Mock
    private CheckResultRepository checkResultRepository;
    @Mock
    private IncidentRepository incidentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PdfReportService pdfReportService;

    @InjectMocks
    private ReportService reportService;

    private User testUser;
    private Monitor testMonitor;

    @BeforeEach
    void setUp() {
        testUser = new User("reporter@cloudpulse.io", "hashedPass", "John", "Reporter", Role.ROLE_USER);
        ReflectionTestUtils.setField(testUser, "id", 10L);

        testMonitor = new Monitor(testUser, "Billing API", "https://api.cloudpulse.io/billing", HttpMethod.GET, 60, 5000, 200);
        ReflectionTestUtils.setField(testMonitor, "id", 100L);

        UserPrincipal principal = UserPrincipal.create(testUser);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void generateMonitorReport_Success() {
        Instant now = Instant.now();
        Instant thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);

        when(monitorRepository.findByIdAndUserId(100L, 10L)).thenReturn(Optional.of(testMonitor));
        when(checkResultRepository.findByMonitorIdAndCheckedAtBetween(eq(100L), any(), any()))
                .thenReturn(List.of());
        when(incidentRepository.findByMonitorIdAndStartedAtBetween(eq(100L), any(), any()))
                .thenReturn(List.of());
        when(pdfReportService.generateSingleMonitorReport(eq(testMonitor), any(), any(), any(), any()))
                .thenReturn(new byte[]{1, 2, 3});

        byte[] result = reportService.generateMonitorReport(100L, thirtyDaysAgo, now);

        assertNotNull(result);
        assertEquals(3, result.length);
        verify(pdfReportService).generateSingleMonitorReport(eq(testMonitor), any(), any(), any(), any());
    }

    @Test
    void generateMonitorReport_NotFound() {
        when(monitorRepository.findByIdAndUserId(999L, 10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                reportService.generateMonitorReport(999L, null, null));
    }

    @Test
    void generateMonitorReport_InvalidDateRange_StartAfterEnd() {
        when(monitorRepository.findByIdAndUserId(100L, 10L)).thenReturn(Optional.of(testMonitor));

        Instant now = Instant.now();
        Instant future = now.plus(5, ChronoUnit.DAYS);

        assertThrows(BadRequestException.class, () ->
                reportService.generateMonitorReport(100L, future, now));
    }

    @Test
    void generateMonitorReport_DateRangeExceeds365Days() {
        when(monitorRepository.findByIdAndUserId(100L, 10L)).thenReturn(Optional.of(testMonitor));

        Instant now = Instant.now();
        Instant fourHundredDaysAgo = now.minus(400, ChronoUnit.DAYS);

        assertThrows(BadRequestException.class, () ->
                reportService.generateMonitorReport(100L, fourHundredDaysAgo, now));
    }

    @Test
    void generateSummaryReport_Success() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(monitorRepository.findByUserId(10L)).thenReturn(List.of(testMonitor));
        when(checkResultRepository.findByMonitorIdAndCheckedAtBetween(eq(100L), any(), any()))
                .thenReturn(List.of());
        when(incidentRepository.findByUserIdAndStartedAtBetween(eq(10L), any(), any()))
                .thenReturn(List.of());
        when(pdfReportService.generateSummaryReport(eq(testUser), any(), any(), anyList(), anyList()))
                .thenReturn(new byte[]{4, 5, 6});

        byte[] result = reportService.generateSummaryReport(null, null);

        assertNotNull(result);
        assertEquals(3, result.length);
    }

    @Test
    void generateIncidentReport_Success() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(incidentRepository.findByUserIdAndStartedAtBetween(eq(10L), any(), any()))
                .thenReturn(List.of());
        when(pdfReportService.generateIncidentReport(eq(testUser), any(), any(), anyList()))
                .thenReturn(new byte[]{7, 8, 9});

        byte[] result = reportService.generateIncidentReport(null, null);

        assertNotNull(result);
        assertEquals(3, result.length);
    }

    @Test
    void generatePerformanceReport_Success() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(monitorRepository.findByUserId(10L)).thenReturn(List.of(testMonitor));
        when(checkResultRepository.findByMonitorIdAndCheckedAtBetween(eq(100L), any(), any()))
                .thenReturn(List.of());
        when(pdfReportService.generatePerformanceReport(eq(testUser), any(), any(), anyList()))
                .thenReturn(new byte[]{10, 11, 12});

        byte[] result = reportService.generatePerformanceReport(null, null);

        assertNotNull(result);
        assertEquals(3, result.length);
    }

    @Test
    void computeMonitorStats_CalculatesCorrectPercentilesAndUptime() {
        Instant now = Instant.now();
        List<CheckResult> results = new ArrayList<>();

        // Add 10 results: 9 SUCCESS (latencies: 10, 20, 30, 40, 50, 60, 70, 80, 200 ms), 1 FAILURE
        long[] lats = {10, 20, 30, 40, 50, 60, 70, 80, 200};
        for (long lat : lats) {
            CheckResult cr = new CheckResult(testMonitor, CheckStatus.SUCCESS, 200, lat, null, null, "127.0.0.1");
            results.add(cr);
        }
        CheckResult failCr = new CheckResult(testMonitor, CheckStatus.FAILURE, 500, 150L, "Internal Server Error", null, "127.0.0.1");
        results.add(failCr);

        when(checkResultRepository.findByMonitorIdAndCheckedAtBetween(eq(100L), any(), any()))
                .thenReturn(results);

        MonitorStats stats = reportService.computeMonitorStats(testMonitor, now.minus(1, ChronoUnit.DAYS), now);

        assertEquals(10, stats.getTotalChecks());
        assertEquals(9, stats.getSuccessChecks());
        assertEquals(1, stats.getFailureChecks());
        assertEquals(90.0, stats.getUptimePercentage());
        assertEquals(10.0, stats.getFailureRate());
        assertEquals(10L, stats.getMinLatencyMs());
        assertEquals(200L, stats.getMaxLatencyMs());
        assertNotNull(stats.getP95LatencyMs());
        assertNotNull(stats.getP99LatencyMs());
    }

    @Test
    void computeMonitorStats_EmptyResults() {
        when(checkResultRepository.findByMonitorIdAndCheckedAtBetween(eq(100L), any(), any()))
                .thenReturn(List.of());

        MonitorStats stats = reportService.computeMonitorStats(testMonitor, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now());

        assertEquals(0, stats.getTotalChecks());
        assertEquals(0, stats.getSuccessChecks());
        assertEquals(0, stats.getFailureChecks());
        assertNull(stats.getUptimePercentage());
        assertNull(stats.getAvgLatencyMs());
    }
}
