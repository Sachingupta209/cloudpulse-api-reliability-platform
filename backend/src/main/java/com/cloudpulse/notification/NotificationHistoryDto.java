package com.cloudpulse.notification;

import java.time.Instant;

public record NotificationHistoryDto(
        Long id,
        Long monitorId,
        String monitorName,
        String monitorUrl,
        Long incidentId,
        String incidentTitle,
        NotificationType notificationType,
        String recipient,
        String subject,
        NotificationStatus status,
        String failureReason,
        int retryCount,
        Instant sentAt,
        Instant createdAt
) {
    public static NotificationHistoryDto from(NotificationHistory history) {
        Long monitorId = history.getMonitor() != null ? history.getMonitor().getId() : null;
        String monitorName = history.getMonitor() != null ? history.getMonitor().getName() : null;
        String monitorUrl = history.getMonitor() != null ? history.getMonitor().getUrl() : null;

        Long incidentId = history.getIncident() != null ? history.getIncident().getId() : null;
        String incidentTitle = history.getIncident() != null ? history.getIncident().getTitle() : null;

        return new NotificationHistoryDto(
                history.getId(),
                monitorId,
                monitorName,
                monitorUrl,
                incidentId,
                incidentTitle,
                history.getNotificationType(),
                history.getRecipient(),
                history.getSubject(),
                history.getStatus(),
                history.getFailureReason(),
                history.getRetryCount(),
                history.getSentAt() != null ? history.getSentAt() : history.getCreatedAt(),
                history.getCreatedAt()
        );
    }
}
