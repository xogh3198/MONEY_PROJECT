package com.dividendbot.news.domain.repository;

import com.dividendbot.news.domain.entity.NewsArticleVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NewsArticleVoteRepository extends JpaRepository<NewsArticleVote, UUID> {
    Optional<NewsArticleVote> findByArticleIdAndUserId(UUID articleId, UUID userId);
}
