package com.dividendbot.news.service;

import com.dividendbot.news.domain.entity.NewsArticle;
import com.dividendbot.news.domain.entity.NewsCategory;
import com.dividendbot.news.domain.repository.NewsArticleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NewsServiceTest {

    @Test
    void searchesNewsByNormalizedQueryAndCategory() {
        NewsArticleRepository repository = mock(NewsArticleRepository.class);
        NewsService service = new NewsService(repository);
        NewsArticle article = NewsArticle.builder()
                .title("원달러 환율 변동")
                .category(NewsCategory.FOREX)
                .build();
        Page<NewsArticle> page = new PageImpl<>(List.of(article));
        Pageable pageable = Pageable.ofSize(20);
        when(repository.search("환율", NewsCategory.FOREX, pageable)).thenReturn(page);

        assertThat(service.search("  환율  ", NewsCategory.FOREX, pageable).getContent())
                .containsExactly(article);
        verify(repository).search("환율", NewsCategory.FOREX, pageable);
    }

    @Test
    void rejectsSearchQueriesShorterThanTwoCharacters() {
        NewsArticleRepository repository = mock(NewsArticleRepository.class);
        NewsService service = new NewsService(repository);

        assertThatThrownBy(() -> service.search("금", null, Pageable.ofSize(20)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("두 글자");
        verify(repository, never()).search(any(), any(), any());
    }

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
