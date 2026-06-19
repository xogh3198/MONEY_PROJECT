package com.dividendbot.news.domain.repository;

import com.dividendbot.news.domain.entity.NewsArticle;
import com.dividendbot.news.domain.entity.NewsCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, UUID> {
    Page<NewsArticle> findByCategory(NewsCategory category, Pageable pageable);
    List<NewsArticle> findTop10ByOrderByViewCountDesc();
    List<NewsArticle> findTop10ByCategoryOrderByViewCountDesc(NewsCategory category);
    boolean existsByTitle(String title);
    boolean existsBySourceUrl(String sourceUrl);

    @Query("SELECT a FROM NewsArticle a WHERE a.publishedAt >= :since ORDER BY (a.viewCount + a.positiveVotes * 2 + a.commentCount * 3) DESC")
    List<NewsArticle> findHotArticlesSince(LocalDateTime since, Pageable pageable);

    @Query("SELECT a FROM NewsArticle a WHERE a.category = :category AND a.publishedAt >= :since ORDER BY (a.viewCount + a.positiveVotes * 2 + a.commentCount * 3) DESC")
    List<NewsArticle> findHotArticlesByCategorySince(NewsCategory category, LocalDateTime since, Pageable pageable);
}
