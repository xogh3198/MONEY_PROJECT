package com.dividendbot.news.service;

import com.dividendbot.news.domain.entity.ForumPost;
import com.dividendbot.news.domain.entity.PostCategory;
import com.dividendbot.news.domain.repository.ForumPostRepository;
import com.dividendbot.news.exception.ForumPostNotFoundException;
import com.dividendbot.news.exception.UnauthorizedAccessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ForumPostService {

    private final ForumPostRepository forumPostRepository;

    public ForumPost createPost(UUID userId, String nickname, String title, String content, PostCategory category) {
        ForumPost post = ForumPost.builder()
                .userId(userId)
                .nickname(nickname)
                .title(title)
                .content(content)
                .category(category)
                .build();
        return forumPostRepository.save(post);
    }

    @Transactional(readOnly = true)
    public Page<ForumPost> getPostList(PostCategory category, Pageable pageable) {
        if (category == null) {
            return forumPostRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return forumPostRepository.findByCategoryOrderByCreatedAtDesc(category, pageable);
    }

    public ForumPost getPostDetail(UUID postId) {
        ForumPost post = forumPostRepository.findById(postId)
                .orElseThrow(() -> new ForumPostNotFoundException(postId));
        forumPostRepository.incrementViewCount(postId);
        post.incrementViewCount();
        return post;
    }

    public ForumPost updatePost(UUID postId, UUID userId, String title, String content) {
        ForumPost post = forumPostRepository.findById(postId)
                .orElseThrow(() -> new ForumPostNotFoundException(postId));
        if (!post.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("게시글 수정 권한이 없습니다");
        }
        post.updatePost(title, content);
        return forumPostRepository.save(post);
    }

    public void deletePost(UUID postId, UUID userId) {
        ForumPost post = forumPostRepository.findById(postId)
                .orElseThrow(() -> new ForumPostNotFoundException(postId));
        if (!post.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("게시글 삭제 권한이 없습니다");
        }
        forumPostRepository.delete(post);
    }

    @Transactional(readOnly = true)
    public List<ForumPost> getPopularPosts(PostCategory category) {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        Pageable top20 = PageRequest.of(0, 20);
        if (category == null) {
            return forumPostRepository.findPopularPosts(since, top20);
        }
        return forumPostRepository.findPopularPostsByCategory(category, since, top20);
    }
}
