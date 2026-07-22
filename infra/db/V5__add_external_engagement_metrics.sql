ALTER TABLE news_articles
    ADD COLUMN IF NOT EXISTS external_view_count BIGINT,
    ADD COLUMN IF NOT EXISTS external_comment_count BIGINT,
    ADD COLUMN IF NOT EXISTS external_positive_count BIGINT,
    ADD COLUMN IF NOT EXISTS external_negative_count BIGINT,
    ADD COLUMN IF NOT EXISTS external_engagement_score INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS external_metric_provider VARCHAR(40),
    ADD COLUMN IF NOT EXISTS external_metric_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS external_metrics_updated_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS external_search_interest INTEGER,
    ADD COLUMN IF NOT EXISTS external_search_interest_source VARCHAR(40),
    ADD COLUMN IF NOT EXISTS external_search_interest_updated_at TIMESTAMP;

UPDATE news_articles
SET external_engagement_score = 0
WHERE external_engagement_score IS NULL;

UPDATE news_articles
SET external_metric_status = 'PENDING'
WHERE external_metric_status IS NULL;

-- Hibernate가 이전 실패한 기동에서 컬럼만 nullable로 생성했더라도 복구한다.
ALTER TABLE news_articles
    ALTER COLUMN external_engagement_score SET DEFAULT 0,
    ALTER COLUMN external_engagement_score SET NOT NULL,
    ALTER COLUMN external_metric_status SET DEFAULT 'PENDING',
    ALTER COLUMN external_metric_status SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_news_articles_external_metrics_refresh
    ON news_articles(external_metrics_updated_at, published_at DESC);
