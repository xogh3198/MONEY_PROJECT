package com.dividendbot.news.domain.repository;

import com.dividendbot.news.domain.entity.ForumComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ForumCommentRepository extends JpaRepository<ForumComment, UUID> {
    Page<ForumComment> findByArticleIdOrderByCreatedAtDesc(UUID articleId, Pageable pageable);
    Page<ForumComment> findByPostIdOrderByCreatedAtAsc(UUID postId, Pageable pageable);
    long countByArticleId(UUID articleId);
}
