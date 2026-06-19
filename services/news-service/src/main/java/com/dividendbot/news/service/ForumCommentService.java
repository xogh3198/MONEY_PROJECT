package com.dividendbot.news.service;

import com.dividendbot.news.domain.entity.ForumComment;
import com.dividendbot.news.domain.entity.ForumPost;
import com.dividendbot.news.domain.repository.ForumCommentRepository;
import com.dividendbot.news.domain.repository.ForumPostRepository;
import com.dividendbot.news.exception.ForumPostNotFoundException;
import com.dividendbot.news.exception.UnauthorizedAccessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ForumCommentService {

    private final ForumCommentRepository forumCommentRepository;
    private final ForumPostRepository forumPostRepository;

    public ForumComment addComment(UUID postId, UUID userId, String username, String content, UUID parentCommentId) {
        ForumPost post = forumPostRepository.findById(postId)
                .orElseThrow(() -> new ForumPostNotFoundException(postId));

        ForumComment comment = ForumComment.builder()
                .postId(postId)
                .userId(userId)
                .username(username)
                .content(content)
                .parentCommentId(parentCommentId)
                .build();

        ForumComment saved = forumCommentRepository.save(comment);
        post.incrementCommentCount();
        forumPostRepository.save(post);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<ForumComment> getComments(UUID postId, Pageable pageable) {
        return forumCommentRepository.findByPostIdOrderByCreatedAtAsc(postId, pageable);
    }

    public ForumComment updateComment(UUID commentId, UUID userId, String content) {
        ForumComment comment = forumCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다: " + commentId));
        if (!comment.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("댓글 수정 권한이 없습니다");
        }
        comment.updateContent(content);
        return forumCommentRepository.save(comment);
    }

    public void deleteComment(UUID commentId, UUID userId) {
        ForumComment comment = forumCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다: " + commentId));
        if (!comment.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("댓글 삭제 권한이 없습니다");
        }

        // Decrement post comment count
        if (comment.getPostId() != null) {
            forumPostRepository.findById(comment.getPostId()).ifPresent(post -> {
                post.decrementCommentCount();
                forumPostRepository.save(post);
            });
        }

        forumCommentRepository.delete(comment);
    }
}
