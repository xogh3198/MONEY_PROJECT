package com.dividendbot.news.controller;

import com.dividendbot.news.domain.entity.NewsArticle;
import com.dividendbot.news.domain.entity.NewsCategory;
import com.dividendbot.news.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NewsController {

    private final NewsService newsService;

    @GetMapping
    public ResponseEntity<Page<NewsArticle>> getArticles(
            @RequestParam(required = false) NewsCategory category,
            Pageable pageable) {
        Page<NewsArticle> articles = (category != null)
                ? newsService.getByCategory(category, pageable)
                : newsService.getAll(pageable);
        return ResponseEntity.ok(articles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NewsArticle> getArticle(@PathVariable UUID id) {
        return ResponseEntity.ok(newsService.getById(id));
    }

    @GetMapping("/hot")
    public ResponseEntity<?> getHotArticles() {
        return ResponseEntity.ok(newsService.getHotArticles());
    }

    @PostMapping("/{id}/vote")
    public ResponseEntity<Void> vote(
            @PathVariable UUID id,
            @RequestParam String type) { // "positive" or "negative"
        newsService.vote(id, type);
        return ResponseEntity.ok().build();
    }
}
