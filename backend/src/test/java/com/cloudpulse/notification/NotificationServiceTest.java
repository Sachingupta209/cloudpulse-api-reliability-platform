package com.cloudpulse.notification;

import com.cloudpulse.incident.Incident;
import com.cloudpulse.monitor.HttpMethod;
import com.cloudpulse.monitor.Monitor;
import com.cloudpulse.user.Role;
import com.cloudpulse.user.User;
import com.cloudpulse.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationPreferenceRepository preferenceRepository;
    @Mock
    private NotificationHistoryRepository historyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailNotificationService emailService;

    private NotificationService notificationService;
    private User testUser;
    private Monitor testMonitor;
    private Incident testIncident;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                preferenceRepository,
                historyRepository,
                userRepository,
                emailService
        );

        testUser = new User("dev@cloudpulse.io", "passHash", "Jane", "Dev", Role.ROLE_USER);
        testUser.setId(101L);

        testMonitor = new Monitor(testUser, "Production API", "https://api.cloudpulse.io/v1", HttpMethod.GET, 60, 5000, 200);
        testMonitor.setId(501L);

        testIncident = new Incident(testMonitor, testUser, "Downtime detected for 'Production API'", "HTTP 500", Instant.now());
        testIncident.setId(901L);
    }

    @Test
    @DisplayName("Should create default preferences when user has none")
    void shouldCreateDefaultPreferencesWhenNoneExist() {
        when(preferenceRepository.findByUserId(101L)).thenReturn(Optional.empty());
        when(userRepository.findById(101L)).thenReturn(Optional.of(testUser));
        when(preferenceRepository.save(any(NotificationPreference.class))).thenAnswer(inv -> {
            NotificationPreference pref = inv.getArgument(0);
            pref.setId(1L);
            return pref;
        });

        NotificationPreferenceDto dto = notificationService.getOrCreatePreferences(101L);

        assertThat(dto).isNotNull();
        assertThat(dto.emailEnabled()).isTrue();
        assertThat(dto.notifyOnIncidentOpened()).isTrue();
        assertThat(dto.notifyOnIncidentResolved()).isTrue();
        assertThat(dto.notifyOnUnhealthy()).isTrue();
        assertThat(dto.notifyOnRecovery()).isTrue();
        assertThat(dto.notificationEmail()).isEqualTo("dev@cloudpulse.io");
        verify(preferenceRepository).save(any(NotificationPreference.class));
    }

    @Test
    @DisplayName("Should update user notification preferences")
    void shouldUpdatePreferences() {
        NotificationPreference existing = new NotificationPreference(testUser);
        existing.setId(1L);
        when(preferenceRepository.findByUserId(101L)).thenReturn(Optional.of(existing));
        when(preferenceRepository.save(any(NotificationPreference.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationPreferenceUpdateRequest request = new NotificationPreferenceUpdateRequest(
                true,
                true,
                false,
                true,
                false,
                "alerts-team@cloudpulse.io"
        );

        NotificationPreferenceDto updated = notificationService.updatePreferences(101L, request);

        assertThat(updated.notifyOnIncidentResolved()).isFalse();
        assertThat(updated.notifyOnRecovery()).isFalse();
        assertThat(updated.notificationEmail()).isEqualTo("alerts-team@cloudpulse.io");
        verify(preferenceRepository).save(existing);
    }

    @Test
    @DisplayName("Should enqueue incident opened notification into outbox with PENDING status")
    void shouldEnqueueIncidentOpenedNotificationWithPendingStatus() {
        NotificationPreference pref = new NotificationPreference(testUser);
        when(preferenceRepository.findByUserId(101L)).thenReturn(Optional.of(pref));
        when(emailService.buildHtmlEmail(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("<html>Alert</html>");

        notificationService.notifyIncidentOpened(testIncident);

        verify(historyRepository).insertOnConflictDoNothing(
                eq(101L),
                eq(501L),
                eq(901L),
                eq("INCIDENT_OPENED"),
                eq("dev@cloudpulse.io"),
                contains("Incident"),
                eq("<html>Alert</html>"),
                anyString(),
                eq("PENDING"),
                eq("incident:opened:901"),
                isNull(),
                eq(0),
                eq(3),
                any(Instant.class)
        );
    }

    @Test
    @DisplayName("Should record SKIPPED status when incident opened notification is disabled in preferences")
    void shouldRecordSkippedWhenIncidentOpenedDisabled() {
        NotificationPreference disabledPref = new NotificationPreference(testUser);
        disabledPref.setNotifyOnIncidentOpened(false);
        when(preferenceRepository.findByUserId(101L)).thenReturn(Optional.of(disabledPref));

        notificationService.notifyIncidentOpened(testIncident);

        verify(historyRepository).insertOnConflictDoNothing(
                eq(101L),
                eq(501L),
                eq(901L),
                eq("INCIDENT_OPENED"),
                eq("dev@cloudpulse.io"),
                contains("Incident"),
                isNull(),
                isNull(),
                eq("SKIPPED"),
                eq("incident:opened:901"),
                contains("Notification disabled"),
                eq(0),
                eq(0),
                any(Instant.class)
        );
    }

    @Test
    @DisplayName("Should enqueue incident resolved notification with calculated downtime")
    void shouldEnqueueIncidentResolvedNotification() {
        NotificationPreference pref = new NotificationPreference(testUser);
        testIncident.resolve(Instant.now());
        when(preferenceRepository.findByUserId(101L)).thenReturn(Optional.of(pref));
        when(emailService.buildHtmlEmail(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("<html>Resolved</html>");

        notificationService.notifyIncidentResolved(testIncident);

        verify(historyRepository).insertOnConflictDoNothing(
                eq(101L),
                eq(501L),
                eq(901L),
                eq("INCIDENT_RESOLVED"),
                eq("dev@cloudpulse.io"),
                contains("Resolved"),
                eq("<html>Resolved</html>"),
                anyString(),
                eq("PENDING"),
                eq("incident:resolved:901"),
                isNull(),
                eq(0),
                eq(3),
                any(Instant.class)
        );
    }

    @Test
    @DisplayName("Should enqueue service unhealthy and recovered notifications")
    void shouldEnqueueServiceUnhealthyAndRecovered() {
        NotificationPreference pref = new NotificationPreference(testUser);
        when(preferenceRepository.findByUserId(101L)).thenReturn(Optional.of(pref));
        when(emailService.buildHtmlEmail(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("<html>Notice</html>");

        notificationService.notifyServiceUnhealthy(testMonitor, "502 Bad Gateway", 7001L);
        notificationService.notifyServiceRecovered(testMonitor, 7002L);

        verify(historyRepository, times(2)).insertOnConflictDoNothing(
                eq(101L),
                eq(501L),
                isNull(),
                anyString(),
                eq("dev@cloudpulse.io"),
                anyString(),
                eq("<html>Notice</html>"),
                anyString(),
                eq("PENDING"),
                anyString(),
                isNull(),
                eq(0),
                eq(3),
                any(Instant.class)
        );
    }

    @Test
    @DisplayName("Should dispatch synchronous test notification and record history")
    void shouldDispatchSynchronousTestNotification() {
        when(userRepository.findById(101L)).thenReturn(Optional.of(testUser));
        when(preferenceRepository.findByUserId(101L)).thenReturn(Optional.of(new NotificationPreference(testUser)));
        when(emailService.buildHtmlEmail(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("<html>Test</html>");
        when(emailService.sendSingleEmail(eq("dev@cloudpulse.io"), anyString(), anyString(), anyString()))
                .thenReturn(new EmailNotificationService.EmailSendResult(true, null));
        when(historyRepository.save(any(NotificationHistory.class))).thenAnswer(inv -> {
            NotificationHistory nh = inv.getArgument(0);
            nh.setId(88L);
            return nh;
        });

        NotificationHistoryDto result = notificationService.sendTestNotification(101L);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(result.notificationType()).isEqualTo(NotificationType.TEST);
        verify(emailService).sendSingleEmail(eq("dev@cloudpulse.io"), anyString(), anyString(), anyString());
        verify(historyRepository).save(any(NotificationHistory.class));
    }

    @Test
    @DisplayName("Should retrieve paginated notification history sorted by createdAt desc")
    void shouldGetNotificationHistory() {
        NotificationHistory historyItem = new NotificationHistory(
                testUser, testMonitor, testIncident,
                NotificationType.INCIDENT_OPENED, "dev@cloudpulse.io",
                "Alert", "<html>", "txt", NotificationStatus.SENT, "key-1", null, 1
        );
        historyItem.setId(10L);

        Page<NotificationHistory> page = new PageImpl<>(List.of(historyItem), PageRequest.of(0, 10), 1);
        when(historyRepository.findByUserIdOrderByCreatedAtDesc(eq(101L), any(Pageable.class))).thenReturn(page);

        var result = notificationService.getNotificationHistory(101L, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().notificationType()).isEqualTo(NotificationType.INCIDENT_OPENED);
        assertThat(result.getContent().getFirst().monitorName()).isEqualTo("Production API");
        verify(historyRepository).findByUserIdOrderByCreatedAtDesc(eq(101L), any(Pageable.class));
    }
}
