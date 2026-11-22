-- ========================================================
-- CloudPulse Notification Outbox & Crash Recovery (Flyway V3)
-- ========================================================

-- Adjust sent_at in notification_history: nullable and no default (set when actually sent)
ALTER TABLE notification_history
    ALTER COLUMN sent_at DROP NOT NULL,
    ALTER COLUMN sent_at DROP DEFAULT,
    ALTER COLUMN status SET DEFAULT 'PENDING';

-- Add columns for transactional outbox, worker crash recovery leases, and content payloads
ALTER TABLE notification_history
    ADD COLUMN IF NOT EXISTS html_body TEXT,
    ADD COLUMN IF NOT EXISTS text_body TEXT,
    ADD COLUMN IF NOT EXISTS max_retries INT NOT NULL DEFAULT 3,
    ADD COLUMN IF NOT EXISTS next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS claimed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS claimed_by VARCHAR(100),
    ADD COLUMN IF NOT EXISTS lease_until TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Partial index for high-throughput atomic claiming of PENDING notifications
CREATE INDEX IF NOT EXISTS idx_notification_history_claim_pending
    ON notification_history(status, next_attempt_at)
    WHERE status = 'PENDING';

-- Partial index for atomic recovery of expired leases on crashed workers
CREATE INDEX IF NOT EXISTS idx_notification_history_claim_expired_lease
    ON notification_history(status, lease_until)
    WHERE status = 'IN_FLIGHT';
