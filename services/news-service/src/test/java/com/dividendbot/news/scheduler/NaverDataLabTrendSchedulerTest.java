package com.dividendbot.news.scheduler;

import com.dividendbot.news.domain.entity.NewsCategory;
import com.dividendbot.news.domain.repository.NewsArticleRepository;
import com.dividendbot.news.service.CollectorRunStateService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
}
