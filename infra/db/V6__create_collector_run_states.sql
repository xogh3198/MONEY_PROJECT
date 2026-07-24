CREATE TABLE IF NOT EXISTS collector_run_states (
    id UUID PRIMARY KEY,
    collector_name VARCHAR(60) NOT NULL,
    status VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    configured BOOLEAN NOT NULL DEFAULT FALSE,
    last_attempt_at TIMESTAMP,
    last_success_at TIMESTAMP,
    last_failure_at TIMESTAMP,
    last_duration_ms BIGINT,
    processed_count INTEGER,
    available_count INTEGER,
    message VARCHAR(500),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_collector_run_states_name UNIQUE (collector_name)
);
