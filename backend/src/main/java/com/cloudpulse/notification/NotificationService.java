package com.cloudpulse.notification;

import com.cloudpulse.common.PagedResponse;
import com.cloudpulse.exception.ResourceNotFoundException;
import com.cloudpulse.incident.Incident;
import com.cloudpulse.monitor.Monitor;
import com.cloudpulse.user.User;
import com.cloudpulse.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final EmailNotificationService emailService;

    public NotificationService(
            NotificationPreferenceRepository preferenceRepository,
            NotificationHistoryRepository historyRepository,
            UserRepository userRepository,
            EmailNotificationService emailService) {
        this.preferenceRepository = preferenceRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Transactional
    public NotificationPreferenceDto getOrCreatePreferences(Long userId) {
        NotificationPreference preference = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
                    NotificationPreference newPref = new NotificationPreference(user);
                    return preferenceRepository.save(newPref);
                });

        return NotificationPreferenceDto.from(preference);
    }

    @Transactional
    public NotificationPreferenceDto updatePreferences(Long userId, NotificationPreferenceUpdateRequest request) {
        NotificationPreference preference = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
                    return new NotificationPreference(user);
                });

        preference.setEmailEnabled(request.emailEnabled());
        preference.setNotifyOnIncidentOpened(request.notifyOnIncidentOpened());
        preference.setNotifyOnIncidentResolved(request.notifyOnIncidentResolved());
        preference.setNotifyOnUnhealthy(request.notifyOnUnhealthy());
        preference.setNotifyOnRecovery(request.notifyOnRecovery());

        if (request.notificationEmail() != null && !request.notificationEmail().isBlank()) {
            preference.setNotificationEmail(request.notificationEmail().trim());
        }

        NotificationPreference saved = preferenceRepository.save(preference);
        log.info("Updated notification preferences for user id={}", userId);
        return NotificationPreferenceDto.from(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<NotificationHistoryDto> getNotificationHistory(Long userId, Pageable pageable) {
        Page<NotificationHistory> page = historyRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PagedResponse.of(page.map(NotificationHistoryDto::from));
    }

    @Transactional
    public NotificationHistoryDto sendTestNotification(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        NotificationPreference preference = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> preferenceRepository.save(new NotificationPreference(user)));

        String recipient = resolveRecipient(preference, user);
        String subject = "[CloudPulse] Test Alert Notification";
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());

        String htmlBody = emailService.buildHtmlEmail(
                "Test Alert Notification",
                "TEST NOTIFICATION",
                "#0284c7",
                "CloudPulse Diagnostic Engine",
                "http://localhost:8081/actuator/health",
                timestamp,
                "This is a test notification confirming that email alerts from your CloudPulse instance are working properly.",
                "http://localhost:5174/settings"
        );

        String textBody = "CloudPulse Test Notification\nYour email notification system is configured correctly.\nSent at: " + timestamp;

        EmailNotificationService.EmailSendResult sendResult = emailService.sendSingleEmail(recipient, subject, htmlBody, textBody);

        NotificationHistory history = new NotificationHistory(
                user,
                null,
                null,
                NotificationType.TEST,
                recipient,
                subject,
                htmlBody,
                textBody,
                sendResult.success() ? NotificationStatus.SENT : NotificationStatus.FAILED,
                "test:" + userId + ":" + System.currentTimeMillis(),
                sendResult.errorMessage(),
                1
        );
        if (sendResult.success()) {
            history.setSentAt(Instant.now());
        }

        NotificationHistory saved = historyRepository.save(history);
        return NotificationHistoryDto.from(saved);
    }

    @Transactional
    public void notifyServiceUnhealthy(Monitor monitor, String failureReason, Long checkResultId) {
        if (monitor == null || monitor.getUser() == null) return;

        User user = monitor.getUser();
        NotificationPreference preference = preferenceRepository.findByUserId(user.getId())
                .orElse(null);

        String idempotencyKey = "unhealthy:monitor:" + monitor.getId() + ":check:" + (checkResultId != null ? checkResultId : System.currentTimeMillis());
        String recipient = resolveRecipient(preference, user);
        String subject = String.format("[CloudPulse Alert] Service '%s' is UNHEALTHY", monitor.getName());
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());

        boolean eligible = preference != null && preference.isEmailEnabled() && preference.isNotifyOnUnhealthy();

        if (!eligible) {
            log.debug("Skipping unhealthy notification for monitor id={} (preferences disabled)", monitor.getId());
            historyRepository.insertOnConflictDoNothing(
                    user.getId(),
                    monitor.getId(),
                    null,
                    NotificationType.SERVICE_UNHEALTHY.name(),
                    recipient,
                    subject,
                    null,
                    null,
                    NotificationStatus.SKIPPED.name(),
                    idempotencyKey,
                    "Notification disabled in user preferences",
                    0,
                    0,
                    Instant.now()
            );
            return;
        }

        String htmlBody = emailService.buildHtmlEmail(
                "Service Health Failure Detected",
                "UNHEALTHY",
                "#ef4444",
                monitor.getName(),
                monitor.getUrl(),
                timestamp,
                "Failure Reason:\n" + (failureReason != null ? failureReason : "Endpoint failed health check or validation rules"),
                "http://localhost:5174/monitors/" + monitor.getId()
        );

        String textBody = String.format("ALERT: Monitor '%s' (%s) is UNHEALTHY.\nReason: %s\nTimestamp: %s",
                monitor.getName(), monitor.getUrl(), failureReason, timestamp);

        historyRepository.insertOnConflictDoNothing(
                user.getId(),
                monitor.getId(),
                null,
                NotificationType.SERVICE_UNHEALTHY.name(),
                recipient,
                subject,
                htmlBody,
                textBody,
                NotificationStatus.PENDING.name(),
                idempotencyKey,
                null,
                0,
                3,
                Instant.now()
        );
    }

    @Transactional
    public void notifyServiceRecovered(Monitor monitor, Long checkResultId) {
        if (monitor == null || monitor.getUser() == null) return;

        User user = monitor.getUser();
        NotificationPreference preference = preferenceRepository.findByUserId(user.getId())
                .orElse(null);

        String idempotencyKey = "recovery:monitor:" + monitor.getId() + ":check:" + (checkResultId != null ? checkResultId : System.currentTimeMillis());
        String recipient = resolveRecipient(preference, user);
        String subject = String.format("[CloudPulse Recovered] Service '%s' is HEALTHY", monitor.getName());
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());

        boolean eligible = preference != null && preference.isEmailEnabled() && preference.isNotifyOnRecovery();

        if (!eligible) {
            log.debug("Skipping recovery notification for monitor id={} (preferences disabled)", monitor.getId());
            historyRepository.insertOnConflictDoNothing(
                    user.getId(),
                    monitor.getId(),
                    null,
                    NotificationType.SERVICE_RECOVERED.name(),
                    recipient,
                    subject,
                    null,
                    null,
                    NotificationStatus.SKIPPED.name(),
                    idempotencyKey,
                    "Notification disabled in user preferences",
                    0,
                    0,
                    Instant.now()
            );
            return;
        }

        String htmlBody = emailService.buildHtmlEmail(
                "Service Health Restored",
                "RECOVERED",
                "#10b981",
                monitor.getName(),
                monitor.getUrl(),
                timestamp,
                "The endpoint is once again responding with expected status codes and passing all smart validation rules.",
                "http://localhost:5174/monitors/" + monitor.getId()
        );

        String textBody = String.format("RECOVERED: Monitor '%s' (%s) has returned to HEALTHY state.\nTimestamp: %s",
                monitor.getName(), monitor.getUrl(), timestamp);

        historyRepository.insertOnConflictDoNothing(
                user.getId(),
                monitor.getId(),
                null,
                NotificationType.SERVICE_RECOVERED.name(),
                recipient,
                subject,
                htmlBody,
                textBody,
                NotificationStatus.PENDING.name(),
                idempotencyKey,
                null,
                0,
                3,
                Instant.now()
        );
    }

    @Transactional
    public void notifyIncidentOpened(Incident incident) {
        if (incident == null || incident.getUser() == null) return;

        User user = incident.getUser();
        NotificationPreference preference = preferenceRepository.findByUserId(user.getId())
                .orElse(null);

        String idempotencyKey = "incident:opened:" + incident.getId();
        String recipient = resolveRecipient(preference, user);
        String monitorName = incident.getMonitor() != null ? incident.getMonitor().getName() : "Unknown Service";
        String monitorUrl = incident.getMonitor() != null ? incident.getMonitor().getUrl() : "";
        String subject = String.format("[CloudPulse Incident] %s", incident.getTitle());
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(incident.getStartedAt());

        boolean eligible = preference != null && preference.isEmailEnabled() && preference.isNotifyOnIncidentOpened();

        if (!eligible) {
            log.debug("Skipping incident opened notification for incident id={} (preferences disabled)", incident.getId());
            historyRepository.insertOnConflictDoNothing(
                    user.getId(),
                    incident.getMonitor() != null ? incident.getMonitor().getId() : null,
                    incident.getId(),
                    NotificationType.INCIDENT_OPENED.name(),
                    recipient,
                    subject,
                    null,
                    null,
                    NotificationStatus.SKIPPED.name(),
                    idempotencyKey,
                    "Notification disabled in user preferences",
                    0,
                    0,
                    Instant.now()
            );
            return;
        }

        String htmlBody = emailService.buildHtmlEmail(
                "Downtime Incident Triggered",
                "INCIDENT OPENED",
                "#ef4444",
                monitorName,
                monitorUrl,
                timestamp,
                "Incident ID: #" + incident.getId() + "\nTitle: " + incident.getTitle() + "\nRoot Cause: " +
                        (incident.getRootCause() != null ? incident.getRootCause() : "Consecutive failures exceeded threshold"),
                "http://localhost:5174/incidents"
        );

        String textBody = String.format("INCIDENT OPENED: %s for '%s'.\nRoot cause: %s\nStarted at: %s",
                incident.getTitle(), monitorName, incident.getRootCause(), timestamp);

        historyRepository.insertOnConflictDoNothing(
                user.getId(),
                incident.getMonitor() != null ? incident.getMonitor().getId() : null,
                incident.getId(),
                NotificationType.INCIDENT_OPENED.name(),
                recipient,
                subject,
                htmlBody,
                textBody,
                NotificationStatus.PENDING.name(),
                idempotencyKey,
                null,
                0,
                3,
                Instant.now()
        );
    }

    @Transactional
    public void notifyIncidentResolved(Incident incident) {
        if (incident == null || incident.getUser() == null) return;

        User user = incident.getUser();
        NotificationPreference preference = preferenceRepository.findByUserId(user.getId())
                .orElse(null);

        String idempotencyKey = "incident:resolved:" + incident.getId();
        String recipient = resolveRecipient(preference, user);
        String monitorName = incident.getMonitor() != null ? incident.getMonitor().getName() : "Unknown Service";
        String monitorUrl = incident.getMonitor() != null ? incident.getMonitor().getUrl() : "";
        String subject = String.format("[CloudPulse Resolved] Incident #%d Resolved: %s", incident.getId(), incident.getTitle());
        String timestamp = incident.getResolvedAt() != null
                ? DateTimeFormatter.ISO_INSTANT.format(incident.getResolvedAt())
                : DateTimeFormatter.ISO_INSTANT.format(Instant.now());

        String downtimeText = incident.getDowntimeSeconds() != null
                ? incident.getDowntimeSeconds() + " seconds"
                : "N/A";

        boolean eligible = preference != null && preference.isEmailEnabled() && preference.isNotifyOnIncidentResolved();

        if (!eligible) {
            log.debug("Skipping incident resolved notification for incident id={} (preferences disabled)", incident.getId());
            historyRepository.insertOnConflictDoNothing(
                    user.getId(),
                    incident.getMonitor() != null ? incident.getMonitor().getId() : null,
                    incident.getId(),
                    NotificationType.INCIDENT_RESOLVED.name(),
                    recipient,
                    subject,
                    null,
                    null,
                    NotificationStatus.SKIPPED.name(),
                    idempotencyKey,
                    "Notification disabled in user preferences",
                    0,
                    0,
                    Instant.now()
            );
            return;
        }

        String htmlBody = emailService.buildHtmlEmail(
                "Downtime Incident Resolved",
                "RESOLVED",
                "#10b981",
                monitorName,
                monitorUrl,
                timestamp,
                "Incident ID: #" + incident.getId() + "\nTitle: " + incident.getTitle() + "\nTotal Downtime: " + downtimeText,
                "http://localhost:5174/incidents"
        );

        String textBody = String.format("INCIDENT RESOLVED: #%d %s.\nTotal Downtime: %s\nResolved at: %s",
                incident.getId(), incident.getTitle(), downtimeText, timestamp);

        historyRepository.insertOnConflictDoNothing(
                user.getId(),
                incident.getMonitor() != null ? incident.getMonitor().getId() : null,
                incident.getId(),
                NotificationType.INCIDENT_RESOLVED.name(),
                recipient,
                subject,
                htmlBody,
                textBody,
                NotificationStatus.PENDING.name(),
                idempotencyKey,
                null,
                0,
                3,
                Instant.now()
        );
    }

    private String resolveRecipient(NotificationPreference preference, User user) {
        if (preference != null && preference.getNotificationEmail() != null && !preference.getNotificationEmail().isBlank()) {
            return preference.getNotificationEmail().trim();
        }
        return user.getEmail();
    }
}
