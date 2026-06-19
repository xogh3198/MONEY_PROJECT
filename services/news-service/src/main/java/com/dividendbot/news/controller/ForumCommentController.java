package com.dividendbot.news.controller;

import com.dividendbot.news.domain.entity.ForumComment;
import com.dividendbot.news.service.ForumCommentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/forum/posts/{postId}/comments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ForumCommentController {

    private final ForumCommentService forumCommentService;

    @GetMapping
    public ResponseEntity<Page<ForumComment>> getComments(
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(forumCommentService.getComments(postId, pageable));
    }

    @PostMapping
    public ResponseEntity<ForumComment> addComment(
            @PathVariable UUID postId,
            @Valid @RequestBody CreateCommentRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId");
        String nickname = (String) httpRequest.getAttribute("nickname");
        ForumComment comment = forumCommentService.addComment(
                postId, userId, nickname, request.content(), request.parentCommentId());
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<ForumComment> updateComment(
            @PathVariable UUID postId,
            @PathVariable UUID commentId,
            @Valid @RequestBody UpdateCommentRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId");
        ForumComment comment = forumCommentService.updateComment(commentId, userId, request.content());
        return ResponseEntity.ok(comment);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID postId,
            @PathVariable UUID commentId,
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId");
        forumCommentService.deleteComment(commentId, userId);
        return ResponseEntity.noContent().build();
    }

    public record CreateCommentRequest(
            @NotBlank(message = "댓글 내용은 비어있을 수 없습니다")
            @Size(max = 1000, message = "댓글은 1000자 이내여야 합니다")
            String content,

            UUID parentCommentId
    ) {}

    public record UpdateCommentRequest(
            @NotBlank(message = "댓글 내용은 비어있을 수 없습니다")
            @Size(max = 1000, message = "댓글은 1000자 이내여야 합니다")
            String content
    ) {}
}
