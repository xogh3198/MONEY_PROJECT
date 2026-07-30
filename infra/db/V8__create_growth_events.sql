CREATE TABLE IF NOT EXISTS growth_events (
    id BIGSERIAL PRIMARY KEY,
    event_name VARCHAR(64) NOT NULL,
    visitor_id_hash VARCHAR(64) NOT NULL,
    session_id_hash VARCHAR(64) NOT NULL,
    path VARCHAR(255) NOT NULL,
    utm_source VARCHAR(120),
    utm_medium VARCHAR(120),
    utm_campaign VARCHAR(120),
    utm_content VARCHAR(120),
    properties_json TEXT NOT NULL DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_growth_events_created_at
    ON growth_events (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_growth_events_event_created
    ON growth_events (event_name, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_growth_events_visitor_created
    ON growth_events (visitor_id_hash, created_at DESC);
