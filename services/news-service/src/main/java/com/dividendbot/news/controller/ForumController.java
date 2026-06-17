package com.dividendbot.news.controller;

import com.dividendbot.news.domain.entity.ForumComment;
import com.dividendbot.news.domain.repository.ForumCommentRepository;
import com.dividendbot.news.domain.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ForumController {

    private final ForumCommentRepository commentRepository;
    private final NewsArticleRepository newsRepository;

    @GetMapping("/comments/{articleId}")
    public ResponseEntity<Page<ForumComment>> getComments(
            @PathVariable UUID articleId, Pageable pageable) {
        return ResponseEntity.ok(
                commentRepository.findByArticleIdOrderByCreatedAtDesc(articleId, pageable));
    }

    @PostMapping("/comments")
    public ResponseEntity<ForumComment> addComment(@RequestBody Map<String, String> body) {
        UUID articleId = UUID.fromString(body.get("articleId"));
        UUID userId = UUID.fromString(body.getOrDefault("userId", UUID.randomUUID().toString()));
        String username = body.getOrDefault("username", "익명");
        String content = body.get("content");

        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        ForumComment comment = ForumComment.builder()
                .articleId(articleId)
                .userId(userId)
                .username(username)
                .content(content)
                .build();

        // 뉴스 기사 댓글 수 증가
        newsRepository.findById(articleId).ifPresent(article -> {
            article.incrementCommentCount();
            newsRepository.save(article);
        });

        return ResponseEntity.status(HttpStatus.CREATED).body(commentRepository.save(comment));
    }
}
