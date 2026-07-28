CREATE TABLE IF NOT EXISTS video_render_jobs (
    id UUID PRIMARY KEY,
    experiment_id VARCHAR(160) NOT NULL,
    title VARCHAR(240) NOT NULL,
    quality VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    stage VARCHAR(80) NOT NULL,
    progress INTEGER NOT NULL DEFAULT 0,
    voice_provider VARCHAR(40),
    output_file_name VARCHAR(255),
    duration_seconds DOUBLE PRECISION,
    asset_credits TEXT,
    error_message VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT chk_video_render_quality CHECK (quality IN ('PREVIEW', 'FINAL')),
    CONSTRAINT chk_video_render_status CHECK (status IN ('QUEUED', 'RENDERING', 'COMPLETED', 'FAILED')),
    CONSTRAINT chk_video_render_progress CHECK (progress BETWEEN 0 AND 100)
);

CREATE INDEX IF NOT EXISTS idx_video_render_jobs_created_at
    ON video_render_jobs(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_video_render_jobs_experiment_id
    ON video_render_jobs(experiment_id);
