package com.cloudpulse.notification;

import com.cloudpulse.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = true;

    @Column(name = "notify_on_incident_opened", nullable = false)
    private boolean notifyOnIncidentOpened = true;

    @Column(name = "notify_on_incident_resolved", nullable = false)
    private boolean notifyOnIncidentResolved = true;

    @Column(name = "notify_on_unhealthy", nullable = false)
    private boolean notifyOnUnhealthy = true;

    @Column(name = "notify_on_recovery", nullable = false)
    private boolean notifyOnRecovery = true;

    @Column(name = "notification_email")
    private String notificationEmail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public NotificationPreference() {
    }

    public NotificationPreference(User user) {
        this.user = user;
        this.emailEnabled = true;
        this.notifyOnIncidentOpened = true;
        this.notifyOnIncidentResolved = true;
        this.notifyOnUnhealthy = true;
        this.notifyOnRecovery = true;
        this.notificationEmail = user != null ? user.getEmail() : null;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public boolean isNotifyOnIncidentOpened() {
        return notifyOnIncidentOpened;
    }

    public void setNotifyOnIncidentOpened(boolean notifyOnIncidentOpened) {
        this.notifyOnIncidentOpened = notifyOnIncidentOpened;
    }

    public boolean isNotifyOnIncidentResolved() {
        return notifyOnIncidentResolved;
    }

    public void setNotifyOnIncidentResolved(boolean notifyOnIncidentResolved) {
        this.notifyOnIncidentResolved = notifyOnIncidentResolved;
    }

    public boolean isNotifyOnUnhealthy() {
        return notifyOnUnhealthy;
    }

    public void setNotifyOnUnhealthy(boolean notifyOnUnhealthy) {
        this.notifyOnUnhealthy = notifyOnUnhealthy;
    }

    public boolean isNotifyOnRecovery() {
        return notifyOnRecovery;
    }

    public void setNotifyOnRecovery(boolean notifyOnRecovery) {
        this.notifyOnRecovery = notifyOnRecovery;
    }

    public String getNotificationEmail() {
        return notificationEmail;
    }

    public void setNotificationEmail(String notificationEmail) {
        this.notificationEmail = notificationEmail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
