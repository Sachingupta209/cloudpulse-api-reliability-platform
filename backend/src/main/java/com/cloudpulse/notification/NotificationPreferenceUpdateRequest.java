package com.cloudpulse.notification;

import jakarta.validation.constraints.Email;

public record NotificationPreferenceUpdateRequest(
        boolean emailEnabled,
        boolean notifyOnIncidentOpened,
        boolean notifyOnIncidentResolved,
        boolean notifyOnUnhealthy,
        boolean notifyOnRecovery,
        @Email(message = "Notification email must be a valid email address")
        String notificationEmail
) {
}
