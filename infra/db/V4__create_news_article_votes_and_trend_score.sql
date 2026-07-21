ALTER TABLE news_articles
    ADD COLUMN IF NOT EXISTS external_trend_score INTEGER NOT NULL DEFAULT 0;

UPDATE news_articles
SET external_trend_score = 0
WHERE external_trend_score IS NULL;

ALTER TABLE news_articles
    ALTER COLUMN external_trend_score SET DEFAULT 0,
    ALTER COLUMN external_trend_score SET NOT NULL;

CREATE TABLE IF NOT EXISTS news_article_votes (
    id UUID PRIMARY KEY,
    article_id UUID NOT NULL,
    user_id UUID NOT NULL,
    vote_type VARCHAR(10) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_news_article_vote UNIQUE (article_id, user_id),
    CONSTRAINT chk_news_article_vote_type CHECK (vote_type IN ('LIKE', 'DISLIKE'))
);

CREATE INDEX IF NOT EXISTS idx_news_article_votes_article_id
    ON news_article_votes(article_id);

CREATE INDEX IF NOT EXISTS idx_news_articles_popularity
    ON news_articles(published_at DESC, view_count DESC, comment_count DESC, positive_votes DESC);
