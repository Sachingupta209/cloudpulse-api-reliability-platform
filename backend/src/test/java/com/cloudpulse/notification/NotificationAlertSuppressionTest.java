package com.cloudpulse.notification;

import com.cloudpulse.incident.Incident;
import com.cloudpulse.incident.IncidentRepository;
import com.cloudpulse.incident.IncidentService;
import com.cloudpulse.incident.IncidentStatus;
import com.cloudpulse.monitor.HttpMethod;
import com.cloudpulse.monitor.Monitor;
import com.cloudpulse.user.Role;
import com.cloudpulse.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationAlertSuppressionTest {

    @Mock
    private IncidentRepository incidentRepository;
    @Mock
    private NotificationService notificationService;

    private IncidentService incidentService;
    private User testUser;
    private Monitor testMonitor;

    @BeforeEach
    void setUp() {
        incidentService = new IncidentService(incidentRepository, notificationService);

        testUser = new User("dev@cloudpulse.io", "pass", "Dev", "User", Role.ROLE_USER);
        testUser.setId(101L);

        testMonitor = new Monitor(testUser, "Payments Gateway", "https://pay.example.com", HttpMethod.GET, 30, 3000, 200);
        testMonitor.setId(555L);
    }

    @Test
    @DisplayName("Alert Suppression: When consecutive failures reach threshold, INCIDENT_OPENED is fired")
    void shouldFireIncidentOpenedWhenThresholdReached() {
        when(incidentRepository.findFirstByMonitorIdAndStatusOrderByStartedAtDesc(555L, IncidentStatus.OPEN))
                .thenReturn(Optional.empty());
        when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> {
            Incident inc = inv.getArgument(0);
            inc.setId(1234L);
            return inc;
        });

        Optional<Incident> result = incidentService.handleFailure(testMonitor, "HTTP 503", 3, 3);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1234L);
        verify(notificationService).notifyIncidentOpened(any(Incident.class));
    }

    @Test
    @DisplayName("Alert Suppression: When consecutive failures are below threshold, no incident is opened")
    void shouldNotFireIncidentWhenBelowThreshold() {
        Optional<Incident> result = incidentService.handleFailure(testMonitor, "HTTP 503", 1, 3);

        assertThat(result).isEmpty();
        verify(notificationService, never()).notifyIncidentOpened(any());
    }

    @Test
    @DisplayName("Alert Suppression: When open incident recovers, INCIDENT_RESOLVED is fired with downtime")
    void shouldFireIncidentResolvedWhenRecovering() {
        Incident openIncident = new Incident(testMonitor, testUser, "Downtime", "503", Instant.now().minusSeconds(120));
        openIncident.setId(1234L);

        when(incidentRepository.findFirstByMonitorIdAndStatusOrderByStartedAtDesc(555L, IncidentStatus.OPEN))
                .thenReturn(Optional.of(openIncident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Incident> result = incidentService.handleRecovery(testMonitor);

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(IncidentStatus.RESOLVED);
        assertThat(result.get().getDowntimeSeconds()).isGreaterThanOrEqualTo(120L);
        verify(notificationService).notifyIncidentResolved(openIncident);
    }

    @Test
    @DisplayName("Alert Suppression: When healthy monitor checks pass without an open incident, no incident resolved alert fires")
    void shouldNotFireResolvedWhenNoIncidentOpen() {
        when(incidentRepository.findFirstByMonitorIdAndStatusOrderByStartedAtDesc(555L, IncidentStatus.OPEN))
                .thenReturn(Optional.empty());

        Optional<Incident> result = incidentService.handleRecovery(testMonitor);

        assertThat(result).isEmpty();
        verify(notificationService, never()).notifyIncidentResolved(any());
    }
}
