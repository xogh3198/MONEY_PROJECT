package com.dividendbot.news.controller;

import com.dividendbot.news.domain.entity.ForumComment;
import com.dividendbot.news.domain.entity.NewsArticle;
import com.dividendbot.news.domain.repository.ForumCommentRepository;
import com.dividendbot.news.domain.repository.NewsArticleRepository;
import jakarta.servlet.http.HttpServletRequest;
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
    public ResponseEntity<ForumComment> addComment(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        UUID articleId = UUID.fromString(body.get("articleId"));
        UUID userId = (UUID) request.getAttribute("userId");
        String username = (String) request.getAttribute("nickname");
        String content = body.get("content");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        NewsArticle article = newsRepository.findById(articleId).orElse(null);
        if (article == null) {
            return ResponseEntity.notFound().build();
        }

        ForumComment comment = ForumComment.builder()
                .articleId(articleId)
                .userId(userId)
                .username(username == null || username.isBlank() ? "회원" : username)
                .content(content.trim())
                .build();

        article.incrementCommentCount();
        newsRepository.save(article);

        return ResponseEntity.status(HttpStatus.CREATED).body(commentRepository.save(comment));
    }
}
