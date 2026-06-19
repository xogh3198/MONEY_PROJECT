package com.dividendbot.news.controller;

import com.dividendbot.news.domain.entity.ForumPost;
import com.dividendbot.news.domain.entity.PostCategory;
import com.dividendbot.news.service.ForumPostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/forum/posts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ForumPostController {

    private final ForumPostService forumPostService;

    @GetMapping
    public ResponseEntity<Page<ForumPost>> getPostList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category) {
        PostCategory postCategory = null;
        if (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category)) {
            postCategory = PostCategory.valueOf(category.toUpperCase());
        }
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(forumPostService.getPostList(postCategory, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ForumPost> getPostDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(forumPostService.getPostDetail(id));
    }

    @GetMapping("/popular")
    public ResponseEntity<List<ForumPost>> getPopularPosts(
            @RequestParam(required = false) String category) {
        PostCategory postCategory = null;
        if (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category)) {
            postCategory = PostCategory.valueOf(category.toUpperCase());
        }
        return ResponseEntity.ok(forumPostService.getPopularPosts(postCategory));
    }

    @PostMapping
    public ResponseEntity<ForumPost> createPost(
            @Valid @RequestBody CreatePostRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId");
        String nickname = (String) httpRequest.getAttribute("nickname");
        ForumPost post = forumPostService.createPost(
                userId, nickname, request.title(), request.content(), request.category());
        return ResponseEntity.status(HttpStatus.CREATED).body(post);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ForumPost> updatePost(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePostRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId");
        ForumPost post = forumPostService.updatePost(id, userId, request.title(), request.content());
        return ResponseEntity.ok(post);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId");
        forumPostService.deletePost(id, userId);
        return ResponseEntity.noContent().build();
    }

    public record CreatePostRequest(
            @NotBlank(message = "제목은 비어있을 수 없습니다")
            @Size(max = 100, message = "제목은 100자 이내여야 합니다")
            String title,

            @NotBlank(message = "본문은 비어있을 수 없습니다")
            @Size(max = 5000, message = "본문은 5000자 이내여야 합니다")
            String content,

            @NotNull(message = "카테고리를 선택해주세요")
            PostCategory category
    ) {}

    public record UpdatePostRequest(
            @NotBlank(message = "제목은 비어있을 수 없습니다")
            @Size(max = 100, message = "제목은 100자 이내여야 합니다")
            String title,

            @NotBlank(message = "본문은 비어있을 수 없습니다")
            @Size(max = 5000, message = "본문은 5000자 이내여야 합니다")
            String content
    ) {}
}
