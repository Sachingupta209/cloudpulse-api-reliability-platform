-- V4__create_status_pages_schema.sql
-- CloudPulse Phase 2.2: Public Status Pages

CREATE TABLE IF NOT EXISTS status_pages (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    slug VARCHAR(100) NOT NULL UNIQUE,
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_status_pages_user_id ON status_pages(user_id);
CREATE INDEX IF NOT EXISTS idx_status_pages_slug ON status_pages(slug);

CREATE TABLE IF NOT EXISTS status_page_monitors (
    id BIGSERIAL PRIMARY KEY,
    status_page_id BIGINT NOT NULL REFERENCES status_pages(id) ON DELETE CASCADE,
    monitor_id BIGINT NOT NULL REFERENCES monitors(id) ON DELETE CASCADE,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_status_page_monitor UNIQUE (status_page_id, monitor_id)
);

CREATE INDEX IF NOT EXISTS idx_spm_status_page_id ON status_page_monitors(status_page_id);
CREATE INDEX IF NOT EXISTS idx_spm_monitor_id ON status_page_monitors(monitor_id);
