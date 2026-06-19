-- V3: Create forum_posts and forum_votes tables, extend forum_comments

CREATE TABLE IF NOT EXISTS forum_posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    category VARCHAR(20) NOT NULL,
    view_count INT NOT NULL DEFAULT 0,
    like_count INT NOT NULL DEFAULT 0,
    dislike_count INT NOT NULL DEFAULT 0,
    comment_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_forum_posts_category ON forum_posts(category);
CREATE INDEX IF NOT EXISTS idx_forum_posts_created_at ON forum_posts(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_forum_posts_user_id ON forum_posts(user_id);

-- Extend forum_comments
ALTER TABLE forum_comments ADD COLUMN IF NOT EXISTS post_id UUID;
ALTER TABLE forum_comments ADD COLUMN IF NOT EXISTS parent_comment_id UUID;
ALTER TABLE forum_comments ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE forum_comments ALTER COLUMN article_id DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_forum_comments_post_id ON forum_comments(post_id);
CREATE INDEX IF NOT EXISTS idx_forum_comments_parent ON forum_comments(parent_comment_id);

-- Create forum_votes
CREATE TABLE IF NOT EXISTS forum_votes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    post_id UUID NOT NULL,
    vote_type VARCHAR(10) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, post_id)
);

CREATE INDEX IF NOT EXISTS idx_forum_votes_post_id ON forum_votes(post_id);
CREATE INDEX IF NOT EXISTS idx_forum_votes_user_post ON forum_votes(user_id, post_id);
