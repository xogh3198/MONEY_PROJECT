-- V2: notification_preferences 테이블 생성
-- 사용자별 배당락일 알림 설정을 저장하는 테이블

CREATE TABLE IF NOT EXISTS notification_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    alert_timing_d7 BOOLEAN NOT NULL DEFAULT FALSE,
    alert_timing_d3 BOOLEAN NOT NULL DEFAULT TRUE,
    alert_timing_d1 BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notification_preferences_user_id ON notification_preferences(user_id);
