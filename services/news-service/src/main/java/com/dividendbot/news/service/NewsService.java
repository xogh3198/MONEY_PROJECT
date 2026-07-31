package com.dividendbot.news.service;

import com.dividendbot.news.domain.entity.NewsArticle;
import com.dividendbot.news.domain.entity.NewsCategory;
import com.dividendbot.news.domain.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsService {

    private final NewsArticleRepository repository;

    public Page<NewsArticle> getAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<NewsArticle> getByCategory(NewsCategory category, Pageable pageable) {
        return repository.findByCategory(category, pageable);
    }

    public Page<NewsArticle> search(String query, NewsCategory category, Pageable pageable) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.length() < 2) {
            throw new IllegalArgumentException("검색어는 두 글자 이상이어야 합니다.");
        }
        return repository.search(normalizedQuery, category, pageable);
    }

    @Transactional
    public NewsArticle getById(UUID id) {
        NewsArticle article = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("기사를 찾을 수 없습니다: " + id));
        article.incrementViewCount();
        return repository.save(article);
    }

    public NewsArticle peekById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("기사를 찾을 수 없습니다: " + id));
    }

    public List<NewsArticle> getHotArticles(NewsCategory category) {
        LocalDateTime since = LocalDateTime.now().minusHours(48);
        org.springframework.data.domain.Pageable top10 = PageRequest.of(0, 10);
        if (category != null) {
            List<NewsArticle> result = repository.findArticleMeasuredHotArticlesByCategorySince(
                    category,
                    since,
                    top10
            );
            if (!result.isEmpty()) {
                return result;
            }
            result = repository.findHotArticlesByCategorySince(category, since, top10);
            if (result.isEmpty()) {
                return repository.findPopularArticlesByCategory(category, top10);
            }
            return result;
        }
        List<NewsArticle> result = repository.findArticleMeasuredHotArticlesSince(since, top10);
        if (!result.isEmpty()) {
            return result;
        }
        result = repository.findHotArticlesSince(since, top10);
        if (result.isEmpty()) {
            return repository.findPopularArticles(top10);
        }
        return result;
    }
}
