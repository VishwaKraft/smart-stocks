CREATE TABLE IF NOT EXISTS email_open_events (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    campaign_id BIGINT REFERENCES campaigns (id),
    campaign VARCHAR(255),
    email_id VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent VARCHAR(512) NOT NULL DEFAULT '',
    metadata TEXT,
    opened_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_email_open_events_user_id ON email_open_events (user_id);
CREATE INDEX IF NOT EXISTS idx_email_open_events_campaign_id ON email_open_events (campaign_id);
CREATE INDEX IF NOT EXISTS idx_email_open_events_campaign ON email_open_events (campaign);
CREATE INDEX IF NOT EXISTS idx_email_open_events_opened_at ON email_open_events (opened_at);
