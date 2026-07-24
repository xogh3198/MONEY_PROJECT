package com.dividendbot.news.scheduler;

import com.dividendbot.news.domain.entity.NewsCategory;
import com.dividendbot.news.domain.entity.NewsArticle;
import com.dividendbot.news.domain.repository.NewsArticleRepository;
import com.dividendbot.news.service.CollectorRunStateService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;

class NaverDataLabTrendSchedulerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NaverDataLabTrendScheduler scheduler = new NaverDataLabTrendScheduler(
            mock(NewsArticleRepository.class),
            mock(CollectorRunStateService.class)
    );

    @Test
    void readsLatestCategoryRatiosAndRoundsThem() throws Exception {
        JsonNode response = objectMapper.readTree("""
                {
                  "results": [
                    {"title":"DOMESTIC","data":[{"period":"2026-07-22","ratio":18.2},{"period":"2026-07-23","ratio":42.6}]},
                    {"title":"FOREX","data":[{"period":"2026-07-23","ratio":100.0}]}
                  ]
                }
                """);

        Map<NewsCategory, Integer> scores = scheduler.readLatestScores(response);

        assertThat(scores)
                .containsEntry(NewsCategory.DOMESTIC, 43)
                .containsEntry(NewsCategory.FOREX, 100);
    }

    @Test
    void ignoresUnknownGroupsAndEmptyData() throws Exception {
        JsonNode response = objectMapper.readTree("""
                {
                  "results": [
                    {"title":"UNKNOWN","data":[{"period":"2026-07-23","ratio":50}]},
                    {"title":"RATE","data":[]}
                  ]
                }
                """);

        assertThat(scheduler.readLatestScores(response)).isEmpty();
    }

    @Test
    void normalizesArticleGroupsAgainstStableReference() throws Exception {
        JsonNode response = objectMapper.readTree("""
                {
                  "results": [
                    {"title":"REFERENCE","data":[{"ratio":40},{"ratio":60}]},
                    {"title":"ARTICLE_0","data":[{"ratio":20},{"ratio":30}]},
                    {"title":"ARTICLE_1","data":[{"ratio":80},{"ratio":100}]}
                  ]
                }
                """);

        assertThat(scheduler.readArticleScores(response))
                .containsEntry("ARTICLE_0", 25)
                .containsEntry("ARTICLE_1", 90);
    }

    @Test
    void extractsDistinctSearchableKeywordsFromArticleTitle() {
        List<String> keywords = scheduler.extractTitleKeywords(
                "[속보] 코스피, 국제유가 급등에 환율 1,466원 돌파"
        );

        assertThat(keywords)
                .contains("코스피 국제유가", "코스피", "국제유가", "급등에", "환율")
                .doesNotContain("속보");
    }

    @Test
    void selectsRecentCandidatesEvenlyAcrossCategories() {
        NewsArticleRepository repository = mock(NewsArticleRepository.class);
        NaverDataLabTrendScheduler balancedScheduler = new NaverDataLabTrendScheduler(
                repository,
                mock(CollectorRunStateService.class)
        );
        LocalDateTime since = LocalDateTime.of(2026, 7, 22, 0, 0);

        for (NewsCategory category : NewsCategory.values()) {
            when(repository.findByCategoryAndPublishedAtAfterOrderByPublishedAtDesc(
                    eq(category),
                    eq(since),
                    any()
            )).thenReturn(List.of(NewsArticle.builder().category(category).build()));
        }

        List<NewsArticle> candidates = balancedScheduler.findBalancedArticleCandidates(since);

        assertThat(candidates)
                .hasSize(NewsCategory.values().length)
                .extracting(NewsArticle::getCategory)
                .containsExactly(NewsCategory.values());
        for (NewsCategory category : NewsCategory.values()) {
            verify(repository).findByCategoryAndPublishedAtAfterOrderByPublishedAtDesc(
                    eq(category),
                    eq(since),
                    any()
            );
        }
    }
}
