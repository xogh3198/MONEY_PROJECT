package com.dividendbot.news.domain.repository;

import com.dividendbot.news.domain.entity.ForumPost;
import com.dividendbot.news.domain.entity.PostCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ForumPostRepository extends JpaRepository<ForumPost, UUID> {
    Page<ForumPost> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<ForumPost> findByCategoryOrderByCreatedAtDesc(PostCategory category, Pageable pageable);

    @Query("SELECT p FROM ForumPost p WHERE p.createdAt >= :since ORDER BY (p.likeCount * 2 + p.viewCount + p.commentCount * 3) DESC")
    List<ForumPost> findPopularPosts(LocalDateTime since, Pageable pageable);

    @Query("SELECT p FROM ForumPost p WHERE p.category = :category AND p.createdAt >= :since ORDER BY (p.likeCount * 2 + p.viewCount + p.commentCount * 3) DESC")
    List<ForumPost> findPopularPostsByCategory(PostCategory category, LocalDateTime since, Pageable pageable);

    @Modifying @Query("UPDATE ForumPost p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    void incrementViewCount(UUID id);
}
