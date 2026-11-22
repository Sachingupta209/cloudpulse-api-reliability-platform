package com.cloudpulse.incident;

import com.cloudpulse.monitor.HttpMethod;
import com.cloudpulse.monitor.Monitor;
import com.cloudpulse.user.Role;
import com.cloudpulse.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;
    @Mock
    private com.cloudpulse.notification.NotificationService notificationService;

    private IncidentService incidentService;
    private Monitor testMonitor;
    private User testUser;

    @BeforeEach
    void setUp() {
        incidentService = new IncidentService(incidentRepository, notificationService);

        testUser = new User("dev@cloudpulse.io", "hashedPass", "Dev", "User", Role.ROLE_USER);
        testUser.setId(1L);

        testMonitor = new Monitor(testUser, "Payments API", "https://api.cloudpulse.io/pay", HttpMethod.GET, 60, 5000, 200);
        testMonitor.setId(10L);
    }

    @Test
    @DisplayName("Should NOT create incident when consecutive failures are below threshold (1 or 2)")
    void shouldNotCreateIncidentBelowThreshold() {
        Optional<Incident> incident1 = incidentService.handleFailure(testMonitor, "Connection timed out", 1, 3);
        Optional<Incident> incident2 = incidentService.handleFailure(testMonitor, "Connection timed out", 2, 3);

        assertThat(incident1).isEmpty();
        assertThat(incident2).isEmpty();
        verify(incidentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create incident when consecutive failures reach threshold of 3")
    void shouldCreateIncidentAtThreshold() {
        when(incidentRepository.findFirstByMonitorIdAndStatusOrderByStartedAtDesc(10L, IncidentStatus.OPEN))
                .thenReturn(Optional.empty());
        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Incident> created = incidentService.handleFailure(testMonitor, "Connection timed out", 3, 3);

        assertThat(created).isPresent();
        assertThat(created.get().getStatus()).isEqualTo(IncidentStatus.OPEN);
        assertThat(created.get().getTitle()).contains("Payments API");
        assertThat(created.get().getRootCause()).isEqualTo("Connection timed out");

        verify(incidentRepository, times(1)).save(any(Incident.class));
    }

    @Test
    @DisplayName("Should NOT create duplicate incident if one is already open")
    void shouldNotCreateDuplicateIncidentIfAlreadyOpen() {
        Incident existingIncident = new Incident(testMonitor, testUser, "Existing incident", "timeout", Instant.now().minusSeconds(120));
        when(incidentRepository.findFirstByMonitorIdAndStatusOrderByStartedAtDesc(10L, IncidentStatus.OPEN))
                .thenReturn(Optional.of(existingIncident));

        Optional<Incident> result = incidentService.handleFailure(testMonitor, "Still failing", 4, 3);

        assertThat(result).isEmpty();
        verify(incidentRepository, never()).save(any(Incident.class));
    }

    @Test
    @DisplayName("Should auto-resolve open incident and calculate downtime when monitor recovers")
    void shouldAutoResolveOpenIncidentOnRecovery() {
        Instant startTime = Instant.now().minusSeconds(180);
        Incident openIncident = new Incident(testMonitor, testUser, "Existing incident", "timeout", startTime);
        when(incidentRepository.findFirstByMonitorIdAndStatusOrderByStartedAtDesc(10L, IncidentStatus.OPEN))
                .thenReturn(Optional.of(openIncident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Incident> resolved = incidentService.handleRecovery(testMonitor);

        assertThat(resolved).isPresent();
        assertThat(resolved.get().getStatus()).isEqualTo(IncidentStatus.RESOLVED);
        assertThat(resolved.get().getResolvedAt()).isNotNull();
        assertThat(resolved.get().getDowntimeSeconds()).isGreaterThanOrEqualTo(180);

        ArgumentCaptor<Incident> captor = ArgumentCaptor.forClass(Incident.class);
        verify(incidentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(IncidentStatus.RESOLVED);
    }
}
