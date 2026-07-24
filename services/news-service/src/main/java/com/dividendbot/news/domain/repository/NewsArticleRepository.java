package com.dividendbot.news.domain.repository;

import com.dividendbot.news.domain.entity.NewsArticle;
import com.dividendbot.news.domain.entity.NewsCategory;
import com.dividendbot.news.domain.entity.ExternalMetricStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, UUID> {
    Page<NewsArticle> findByCategory(NewsCategory category, Pageable pageable);
    boolean existsByTitle(String title);
    boolean existsBySourceUrl(String sourceUrl);
    Optional<NewsArticle> findBySourceUrl(String sourceUrl);
    Optional<NewsArticle> findFirstByTitle(String title);
    long countByViewCountGreaterThan(int viewCount);
    long countByCommentCountGreaterThan(int commentCount);
    long countByExternalSearchInterestIsNotNull();
    long countByExternalMetricStatus(ExternalMetricStatus status);

    @Query("SELECT COUNT(a) FROM NewsArticle a WHERE a.positiveVotes > 0 OR a.negativeVotes > 0")
    long countArticlesWithInternalVotes();

    @Query("SELECT COUNT(a) FROM NewsArticle a WHERE a.externalViewCount IS NOT NULL " +
            "OR a.externalCommentCount IS NOT NULL OR a.externalPositiveCount IS NOT NULL " +
            "OR a.externalNegativeCount IS NOT NULL")
    long countArticlesWithExternalValues();

    @Query("SELECT MAX(a.externalMetricsUpdatedAt) FROM NewsArticle a")
    LocalDateTime findLatestExternalMetricsUpdatedAt();

    @Query("SELECT MAX(a.externalSearchInterestUpdatedAt) FROM NewsArticle a")
    LocalDateTime findLatestSearchInterestUpdatedAt();

    @Query("SELECT a FROM NewsArticle a WHERE a.publishedAt >= :since " +
            "AND (a.externalMetricsUpdatedAt IS NULL OR a.externalMetricsUpdatedAt < :staleBefore) " +
            "ORDER BY (a.viewCount + a.positiveVotes * 5 + a.negativeVotes * 2 + a.commentCount * 6 " +
            "+ COALESCE(a.externalTrendScore, 0) + COALESCE(a.externalSearchInterest, 0)) DESC, " +
            "a.publishedAt DESC")
    List<NewsArticle> findExternalMetricRefreshCandidates(
            LocalDateTime since,
            LocalDateTime staleBefore,
            Pageable pageable
    );

    List<NewsArticle> findByPublishedAtAfter(LocalDateTime since);

    @Query("SELECT a FROM NewsArticle a WHERE a.publishedAt >= :since " +
            "ORDER BY (a.viewCount + a.positiveVotes * 5 + a.negativeVotes * 2 + a.commentCount * 6 " +
            "+ COALESCE(a.externalTrendScore, 0) + COALESCE(a.externalEngagementScore, 0) " +
            "+ COALESCE(a.externalSearchInterest, 0) * 100) DESC, a.publishedAt DESC")
    List<NewsArticle> findHotArticlesSince(LocalDateTime since, Pageable pageable);

    @Query("SELECT a FROM NewsArticle a WHERE a.category = :category AND a.publishedAt >= :since " +
            "ORDER BY (a.viewCount + a.positiveVotes * 5 + a.negativeVotes * 2 + a.commentCount * 6 " +
            "+ COALESCE(a.externalTrendScore, 0) + COALESCE(a.externalEngagementScore, 0) " +
            "+ COALESCE(a.externalSearchInterest, 0) * 100) DESC, a.publishedAt DESC")
    List<NewsArticle> findHotArticlesByCategorySince(NewsCategory category, LocalDateTime since, Pageable pageable);

    @Query("SELECT a FROM NewsArticle a " +
            "ORDER BY (a.viewCount + a.positiveVotes * 5 + a.negativeVotes * 2 + a.commentCount * 6 " +
            "+ COALESCE(a.externalTrendScore, 0) + COALESCE(a.externalEngagementScore, 0) " +
            "+ COALESCE(a.externalSearchInterest, 0) * 100) DESC, a.publishedAt DESC")
    List<NewsArticle> findPopularArticles(Pageable pageable);

    @Query("SELECT a FROM NewsArticle a WHERE a.category = :category " +
            "ORDER BY (a.viewCount + a.positiveVotes * 5 + a.negativeVotes * 2 + a.commentCount * 6 " +
            "+ COALESCE(a.externalTrendScore, 0) + COALESCE(a.externalEngagementScore, 0) " +
            "+ COALESCE(a.externalSearchInterest, 0) * 100) DESC, a.publishedAt DESC")
    List<NewsArticle> findPopularArticlesByCategory(NewsCategory category, Pageable pageable);
}
