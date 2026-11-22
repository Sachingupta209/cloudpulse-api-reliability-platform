-- V5: Create Anomalies Schema for Phase 2.4 Advanced Analytics & Anomaly Detection

CREATE TABLE anomalies (
    id BIGSERIAL PRIMARY KEY,
    monitor_id BIGINT NOT NULL REFERENCES monitors(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    anomaly_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    observed_value DOUBLE PRECISION NOT NULL,
    baseline_value DOUBLE PRECISION NOT NULL,
    description TEXT NOT NULL,
    detected_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    acknowledged_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_anomalies_user_detected ON anomalies(user_id, detected_at DESC);
CREATE INDEX idx_anomalies_monitor_detected ON anomalies(monitor_id, detected_at DESC);
CREATE INDEX idx_anomalies_acknowledged ON anomalies(acknowledged);
