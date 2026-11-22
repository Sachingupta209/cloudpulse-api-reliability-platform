-- ========================================================
-- CloudPulse Notification Module Schema (Flyway V2)
-- ========================================================

-- Notification Preferences Table
CREATE TABLE IF NOT EXISTS notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    notify_on_incident_opened BOOLEAN NOT NULL DEFAULT TRUE,
    notify_on_incident_resolved BOOLEAN NOT NULL DEFAULT TRUE,
    notify_on_unhealthy BOOLEAN NOT NULL DEFAULT TRUE,
    notify_on_recovery BOOLEAN NOT NULL DEFAULT TRUE,
    notification_email VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Notification History Table
CREATE TABLE IF NOT EXISTS notification_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    monitor_id BIGINT REFERENCES monitors(id) ON DELETE SET NULL,
    incident_id BIGINT REFERENCES incidents(id) ON DELETE SET NULL,
    notification_type VARCHAR(50) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SENT',
    idempotency_key VARCHAR(255) UNIQUE,
    failure_reason TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    sent_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Performance Indexes
CREATE INDEX IF NOT EXISTS idx_notification_preferences_user_id ON notification_preferences(user_id);
CREATE INDEX IF NOT EXISTS idx_notification_history_user_id ON notification_history(user_id);
CREATE INDEX IF NOT EXISTS idx_notification_history_monitor_id ON notification_history(monitor_id);
CREATE INDEX IF NOT EXISTS idx_notification_history_incident_id ON notification_history(incident_id);
CREATE INDEX IF NOT EXISTS idx_notification_history_idempotency ON notification_history(idempotency_key);
CREATE INDEX IF NOT EXISTS idx_notification_history_created_at ON notification_history(created_at DESC);
