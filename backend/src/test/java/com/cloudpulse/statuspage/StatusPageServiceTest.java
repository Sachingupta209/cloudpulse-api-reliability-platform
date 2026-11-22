package com.cloudpulse.statuspage;

import com.cloudpulse.exception.BadRequestException;
import com.cloudpulse.exception.ResourceNotFoundException;
import com.cloudpulse.incident.Incident;
import com.cloudpulse.incident.IncidentRepository;
import com.cloudpulse.incident.IncidentStatus;
import com.cloudpulse.monitor.HealthStatus;
import com.cloudpulse.monitor.Monitor;
import com.cloudpulse.monitor.MonitorRepository;
import com.cloudpulse.monitor.MonitorStatus;
import com.cloudpulse.result.CheckResultRepository;
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
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatusPageServiceTest {

    @Mock
    private StatusPageRepository statusPageRepository;
    @Mock
    private StatusPageMonitorRepository statusPageMonitorRepository;
    @Mock
    private MonitorRepository monitorRepository;
    @Mock
    private CheckResultRepository checkResultRepository;
    @Mock
    private IncidentRepository incidentRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StatusPageService statusPageService;

    private User testUser;
    private Monitor testMonitor1;
    private Monitor testMonitor2;

    @BeforeEach
    void setUp() {
        UserPrincipal principal = new UserPrincipal(100L, "owner@cloudpulse.io", "hash", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        testUser = new User("owner@cloudpulse.io", "hash", "Alice", "Smith", Role.ROLE_USER);
        ReflectionTestUtils.setField(testUser, "id", 100L);

        testMonitor1 = new Monitor(testUser, "API Gateway", "https://api.example.com", null, 60, 5000, 200);
        ReflectionTestUtils.setField(testMonitor1, "id", 1L);
        testMonitor1.setHealthStatus(HealthStatus.HEALTHY);

        testMonitor2 = new Monitor(testUser, "Auth Service", "https://auth.example.com", null, 60, 5000, 200);
        ReflectionTestUtils.setField(testMonitor2, "id", 2L);
        testMonitor2.setHealthStatus(HealthStatus.UNHEALTHY);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCreateStatusPage_DefaultUnpublished_AutoSlug() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));
        when(statusPageRepository.existsBySlug(anyString())).thenReturn(false);
        when(monitorRepository.findAllById(List.of(1L))).thenReturn(List.of(testMonitor1));
        when(statusPageRepository.save(any(StatusPage.class))).thenAnswer(inv -> {
            StatusPage sp = inv.getArgument(0);
            ReflectionTestUtils.setField(sp, "id", 50L);
            return sp;
        });

        StatusPageRequest req = new StatusPageRequest("System Status", "Live metrics", null, null, List.of(1L));
        StatusPageResponse res = statusPageService.createStatusPage(req);

        assertNotNull(res);
        assertEquals(50L, res.getId());
        assertEquals("System Status", res.getTitle());
        assertFalse(res.isPublished(), "Status page must default to unpublished (false)");
        assertTrue(res.getSlug().startsWith("sp-"));
        verify(statusPageMonitorRepository).save(any(StatusPageMonitor.class));
    }

    @Test
    void testCreateStatusPage_CustomSlug_Valid() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));
        when(statusPageRepository.existsBySlug("acme-status")).thenReturn(false);
        when(statusPageRepository.save(any(StatusPage.class))).thenAnswer(inv -> inv.getArgument(0));

        StatusPageRequest req = new StatusPageRequest("Acme", "Desc", "acme-status", true, Collections.emptyList());
        StatusPageResponse res = statusPageService.createStatusPage(req);

        assertEquals("acme-status", res.getSlug());
        assertTrue(res.isPublished());
    }

    @Test
    void testCreateStatusPage_InvalidCustomSlug_Throws() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));

        StatusPageRequest req = new StatusPageRequest("Acme", "Desc", "-bad_slug-", true, Collections.emptyList());
        assertThrows(BadRequestException.class, () -> statusPageService.createStatusPage(req));

        StatusPageRequest reservedReq = new StatusPageRequest("Acme", "Desc", "admin", true, Collections.emptyList());
        assertThrows(BadRequestException.class, () -> statusPageService.createStatusPage(reservedReq));
    }

    @Test
    void testCreateStatusPage_CrossUserMonitor_Throws() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));

        User otherUser = new User("other@cloudpulse.io", "hash", "Bob", "Jones", Role.ROLE_USER);
        ReflectionTestUtils.setField(otherUser, "id", 200L);
        Monitor otherMonitor = new Monitor(otherUser, "Foreign API", "https://foreign.com", null, 60, 5000, 200);
        ReflectionTestUtils.setField(otherMonitor, "id", 99L);

        when(monitorRepository.findAllById(List.of(99L))).thenReturn(List.of(otherMonitor));

        StatusPageRequest req = new StatusPageRequest("Status", null, "my-status", true, List.of(99L));
        assertThrows(BadRequestException.class, () -> statusPageService.createStatusPage(req));
    }

    @Test
    void testGetPublicStatusPage_Unpublished_ThrowsNotFound() {
        StatusPage sp = new StatusPage(testUser, "Private Status", null, "sp-private", false);
        when(statusPageRepository.findBySlug("sp-private")).thenReturn(Optional.of(sp));

        assertThrows(ResourceNotFoundException.class, () -> statusPageService.getPublicStatusPage("sp-private"));
    }

    @Test
    void testGetPublicStatusPage_Published_CalculatesUptimeAndOverallStatus() {
        StatusPage sp = new StatusPage(testUser, "Public Status", "System health", "sp-public", true);
        ReflectionTestUtils.setField(sp, "id", 10L);

        StatusPageMonitor spm1 = new StatusPageMonitor(sp, testMonitor1, 0);
        StatusPageMonitor spm2 = new StatusPageMonitor(sp, testMonitor2, 1);

        when(statusPageRepository.findBySlug("sp-public")).thenReturn(Optional.of(sp));
        when(statusPageMonitorRepository.findByStatusPageIdOrderByDisplayOrderAsc(10L)).thenReturn(List.of(spm1, spm2));

        // Mock batch aggregated check result stats:
        // monitor 1: 100 checks, 99 success, avg latency 120.5ms
        // monitor 2: 50 checks, 40 success, avg latency 350.2ms
        List<Object[]> rows24h = List.of(
                new Object[]{1L, 100L, 99L, 120.5},
                new Object[]{2L, 50L, 40L, 350.2}
        );
        when(checkResultRepository.aggregateStatsByMonitorIdsSince(anyCollection(), any(Instant.class)))
                .thenReturn(rows24h);

        // Mock recent incident
        Incident inc = new Incident(testMonitor2, testUser, "High latency detected", "Response time exceeded SLA", Instant.now().minus(2, ChronoUnit.HOURS));
        inc.resolve(Instant.now().minus(1, ChronoUnit.HOURS));

        when(incidentRepository.findRecentByMonitorIdsSince(anyCollection(), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(inc));

        PublicStatusPageDto publicPage = statusPageService.getPublicStatusPage("sp-public");

        assertNotNull(publicPage);
        assertEquals("Public Status", publicPage.getTitle());
        // One monitor is healthy, one is unhealthy -> DEGRADED
        assertEquals(PublicSystemStatus.DEGRADED, publicPage.getOverallStatus());
        assertEquals(2, publicPage.getMonitors().size());

        PublicMonitorDto m1 = publicPage.getMonitors().get(0);
        assertEquals("API Gateway", m1.getName());
        assertEquals("OPERATIONAL", m1.getStatus());
        assertEquals(99.0, m1.getUptime24h());
        assertEquals(120.5, m1.getAverageLatencyMs());

        PublicMonitorDto m2 = publicPage.getMonitors().get(1);
        assertEquals("Auth Service", m2.getName());
        assertEquals("DOWN", m2.getStatus());
        assertEquals(80.0, m2.getUptime24h()); // 40/50 * 100

        assertEquals(1, publicPage.getRecentIncidents().size());
        PublicIncidentDto incDto = publicPage.getRecentIncidents().get(0);
        assertEquals("Auth Service", incDto.getMonitorName());
        assertEquals("High latency detected", incDto.getTitle());
        assertEquals("RESOLVED", incDto.getStatus());
        assertEquals(60L, incDto.getDurationMinutes());
    }

    @Test
    void testUptimeCalculation_NoChecks_ReturnsNull() {
        StatusPage sp = new StatusPage(testUser, "Fresh Service", null, "sp-fresh", true);
        ReflectionTestUtils.setField(sp, "id", 11L);

        StatusPageMonitor spm1 = new StatusPageMonitor(sp, testMonitor1, 0);

        when(statusPageRepository.findBySlug("sp-fresh")).thenReturn(Optional.of(sp));
        when(statusPageMonitorRepository.findByStatusPageIdOrderByDisplayOrderAsc(11L)).thenReturn(List.of(spm1));
        when(checkResultRepository.aggregateStatsByMonitorIdsSince(anyCollection(), any(Instant.class)))
                .thenReturn(Collections.emptyList());
        when(incidentRepository.findRecentByMonitorIdsSince(anyCollection(), any(Instant.class), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        PublicStatusPageDto publicPage = statusPageService.getPublicStatusPage("sp-fresh");

        assertNotNull(publicPage);
        PublicMonitorDto m1 = publicPage.getMonitors().get(0);
        assertNull(m1.getUptime24h(), "Uptime must be null when there are 0 checks, not 100%");
        assertNull(m1.getAverageLatencyMs(), "Latency must be null when there are 0 checks");
    }

    @Test
    void testOverallStatus_AllPaused_ReturnsMaintenance() {
        testMonitor1.setStatus(MonitorStatus.PAUSED);
        testMonitor2.setStatus(MonitorStatus.PAUSED);

        StatusPage sp = new StatusPage(testUser, "Maintenance Status", null, "sp-maint", true);
        ReflectionTestUtils.setField(sp, "id", 12L);

        StatusPageMonitor spm1 = new StatusPageMonitor(sp, testMonitor1, 0);
        StatusPageMonitor spm2 = new StatusPageMonitor(sp, testMonitor2, 1);

        when(statusPageRepository.findBySlug("sp-maint")).thenReturn(Optional.of(sp));
        when(statusPageMonitorRepository.findByStatusPageIdOrderByDisplayOrderAsc(12L)).thenReturn(List.of(spm1, spm2));
        when(checkResultRepository.aggregateStatsByMonitorIdsSince(anyCollection(), any(Instant.class)))
                .thenReturn(Collections.emptyList());
        when(incidentRepository.findRecentByMonitorIdsSince(anyCollection(), any(Instant.class), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        PublicStatusPageDto publicPage = statusPageService.getPublicStatusPage("sp-maint");

        assertEquals(PublicSystemStatus.MAINTENANCE, publicPage.getOverallStatus(),
                "When all published monitors are paused, overall status must be MAINTENANCE, not OUTAGE");
    }
}
