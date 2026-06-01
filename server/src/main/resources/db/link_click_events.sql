CREATE TABLE IF NOT EXISTS link_click_events (
    id BIGSERIAL PRIMARY KEY,
    short_id VARCHAR(10) NOT NULL,
    original_url TEXT NOT NULL,
    user_id BIGINT,
    campaign VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent VARCHAR(512) NOT NULL DEFAULT '',
    metadata TEXT,
    clicked_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_link_click_events_short_id ON link_click_events (short_id);
CREATE INDEX IF NOT EXISTS idx_link_click_events_user_id ON link_click_events (user_id);
CREATE INDEX IF NOT EXISTS idx_link_click_events_clicked_at ON link_click_events (clicked_at);
