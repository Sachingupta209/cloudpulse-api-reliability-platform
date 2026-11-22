package com.cloudpulse.notification;

public record NotificationPreferenceDto(
        Long id,
        Long userId,
        boolean emailEnabled,
        boolean notifyOnIncidentOpened,
        boolean notifyOnIncidentResolved,
        boolean notifyOnUnhealthy,
        boolean notifyOnRecovery,
        String notificationEmail
) {
    public static NotificationPreferenceDto from(NotificationPreference preference) {
        return new NotificationPreferenceDto(
                preference.getId(),
                preference.getUser().getId(),
                preference.isEmailEnabled(),
                preference.isNotifyOnIncidentOpened(),
                preference.isNotifyOnIncidentResolved(),
                preference.isNotifyOnUnhealthy(),
                preference.isNotifyOnRecovery(),
                preference.getNotificationEmail()
        );
    }
}
