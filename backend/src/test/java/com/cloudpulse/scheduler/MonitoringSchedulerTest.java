package com.cloudpulse.scheduler;

import com.cloudpulse.monitor.HttpMethod;
import com.cloudpulse.monitor.Monitor;
import com.cloudpulse.monitor.MonitorRepository;
import com.cloudpulse.monitor.MonitorStatus;
import com.cloudpulse.monitoring.MonitoringService;
import com.cloudpulse.user.Role;
import com.cloudpulse.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitoringSchedulerTest {

    @Mock
    private MonitorRepository monitorRepository;

    @Mock
    private MonitoringService monitoringService;

    // Direct (synchronous) executor for testing task execution deterministically
    private final Executor directExecutor = Runnable::run;

    private MonitoringScheduler scheduler;
    private User testUser;

    @BeforeEach
    void setUp() {
        scheduler = new MonitoringScheduler(monitorRepository, monitoringService, directExecutor);
        testUser = new User("scheduler@cloudpulse.io", "pass", "Sam", "Scheduler", Role.ROLE_USER);
        testUser.setId(5L);
    }

    @Test
    @DisplayName("Due active monitors should be scheduled and execute check by ID")
    void shouldExecuteChecksForDueActiveMonitors() {
        Monitor m1 = new Monitor(testUser, "Due API 1", "https://api.example.com/1", HttpMethod.GET, 60, 5000, 200);
        m1.setId(101L);
        m1.setStatus(MonitorStatus.ACTIVE);
        m1.setLastCheckedAt(null); // never checked -> due

        Monitor m2 = new Monitor(testUser, "Due API 2", "https://api.example.com/2", HttpMethod.GET, 60, 5000, 200);
        m2.setId(102L);
        m2.setStatus(MonitorStatus.ACTIVE);
        m2.setLastCheckedAt(Instant.now().minusSeconds(120)); // checked 120s ago (interval 60s) -> due

        when(monitorRepository.findByStatus(MonitorStatus.ACTIVE)).thenReturn(List.of(m1, m2));

        scheduler.scheduleDueChecks();

        verify(monitoringService).executeCheckById(101L);
        verify(monitoringService).executeCheckById(102L);
    }

    @Test
    @DisplayName("Monitors not yet due should be skipped")
    void shouldSkipMonitorsNotDue() {
        Monitor m = new Monitor(testUser, "Recent API", "https://api.example.com/recent", HttpMethod.GET, 60, 5000, 200);
        m.setId(103L);
        m.setStatus(MonitorStatus.ACTIVE);
        m.setLastCheckedAt(Instant.now().minusSeconds(10)); // checked 10s ago (interval 60s) -> NOT due

        when(monitorRepository.findByStatus(MonitorStatus.ACTIVE)).thenReturn(List.of(m));

        scheduler.scheduleDueChecks();

        verify(monitoringService, never()).executeCheckById(any());
    }
}
