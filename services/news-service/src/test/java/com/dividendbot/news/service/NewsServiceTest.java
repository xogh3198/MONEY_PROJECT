package com.dividendbot.news.service;

import com.dividendbot.news.domain.entity.NewsArticle;
import com.dividendbot.news.domain.entity.NewsCategory;
import com.dividendbot.news.domain.repository.NewsArticleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NewsServiceTest {

    @Test
    void prioritizesArticleMeasuredNewsForHotRanking() {
        NewsArticleRepository repository = mock(NewsArticleRepository.class);
        NewsService service = new NewsService(repository);
        NewsArticle measured = NewsArticle.builder()
                .title("기사별 측정 기사")
                .category(NewsCategory.DOMESTIC)
                .build();
        when(repository.findArticleMeasuredHotArticlesSince(
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(List.of(measured));

        assertThat(service.getHotArticles(null)).containsExactly(measured);
        verify(repository, never()).findHotArticlesSince(any(LocalDateTime.class), any(Pageable.class));
    }

    @Test
    void fallsBackToCategoryTrendWhenArticleMeasurementIsUnavailable() {
        NewsArticleRepository repository = mock(NewsArticleRepository.class);
        NewsService service = new NewsService(repository);
        NewsArticle fallback = NewsArticle.builder()
                .title("분야 fallback 기사")
                .category(NewsCategory.CRYPTO)
                .build();
        when(repository.findArticleMeasuredHotArticlesByCategorySince(
                eq(NewsCategory.CRYPTO),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(List.of());
        when(repository.findHotArticlesByCategorySince(
                eq(NewsCategory.CRYPTO),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(List.of(fallback));

        assertThat(service.getHotArticles(NewsCategory.CRYPTO)).containsExactly(fallback);
    }
}
