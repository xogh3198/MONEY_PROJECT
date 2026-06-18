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

    @Transactional
    public NewsArticle getById(UUID id) {
        NewsArticle article = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("기사를 찾을 수 없습니다: " + id));
        article.incrementViewCount();
        return repository.save(article);
    }

    public List<NewsArticle> getHotArticles(NewsCategory category) {
        if (category != null) {
            return repository.findTop10ByCategoryOrderByViewCountDesc(category);
        }
        return repository.findTop10ByOrderByViewCountDesc();
    }

    @Transactional
    public void vote(UUID articleId, String type) {
        NewsArticle article = repository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("기사를 찾을 수 없습니다"));
        if ("positive".equals(type)) {
            article.addPositiveVote();
        } else {
            article.addNegativeVote();
        }
        repository.save(article);
    }
}
