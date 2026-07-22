package com.dividendbot.news.scheduler;

import com.dividendbot.news.domain.entity.NewsArticle;
import com.dividendbot.news.domain.entity.NewsCategory;
import com.dividendbot.news.domain.repository.NewsArticleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "naver.datalab.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class NaverDataLabTrendScheduler {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final List<Map<String, Object>> KEYWORD_GROUPS = List.of(
            group(NewsCategory.DOMESTIC, "코스피", "코스닥", "국내증시"),
            group(NewsCategory.OVERSEAS, "나스닥", "S&P500", "미국증시"),
            group(NewsCategory.FOREX, "원달러 환율", "달러 환율", "환율"),
            group(NewsCategory.RATE, "기준금리", "금리 인하", "FOMC"),
            group(NewsCategory.CRYPTO, "비트코인", "이더리움", "암호화폐")
    );

    private final NewsArticleRepository newsRepository;

    @Value("${naver.api.client-id:}")
    private String clientId;

    @Value("${naver.api.client-secret:}")
    private String clientSecret;

    @Scheduled(fixedDelay = 21_600_000, initialDelay = 240_000)
    public void refreshSearchInterest() {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) return;

        try {
            LocalDate endDate = LocalDate.now(SEOUL).minusDays(1);
            Map<String, Object> request = Map.of(
                    "startDate", endDate.minusDays(29).toString(),
                    "endDate", endDate.toString(),
                    "timeUnit", "date",
                    "keywordGroups", KEYWORD_GROUPS
            );

            JsonNode response = WebClient.builder()
                    .baseUrl("https://openapi.naver.com")
                    .defaultHeader("X-Naver-Client-Id", clientId)
                    .defaultHeader("X-Naver-Client-Secret", clientSecret)
                    .build()
                    .post()
                    .uri("/v1/datalab/search")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(10));

            Map<NewsCategory, Integer> scores = readLatestScores(response);
            if (scores.isEmpty()) return;

            LocalDateTime updatedAt = LocalDateTime.now();
            List<NewsArticle> recent = newsRepository.findByPublishedAtAfter(updatedAt.minusDays(7));
            for (NewsArticle article : recent) {
                Integer score = scores.get(article.getCategory());
                if (score != null) article.updateExternalSearchInterest(score, "NAVER_DATALAB_CATEGORY", updatedAt);
            }
            newsRepository.saveAll(recent);
            log.info("네이버 DataLab 공식 분야 검색 관심도 갱신: 카테고리 {}개, 기사 {}건", scores.size(), recent.size());
        } catch (Exception e) {
            log.warn("네이버 DataLab 검색 관심도 갱신 실패. API 권한과 호출 한도를 확인하세요: {}", e.getMessage());
        }
    }

    private Map<NewsCategory, Integer> readLatestScores(JsonNode response) {
        Map<NewsCategory, Integer> scores = new EnumMap<>(NewsCategory.class);
        if (response == null || !response.path("results").isArray()) return scores;

        for (JsonNode result : response.path("results")) {
            try {
                NewsCategory category = NewsCategory.valueOf(result.path("title").asText());
                JsonNode data = result.path("data");
                if (!data.isArray() || data.isEmpty()) continue;
                double ratio = data.path(data.size() - 1).path("ratio").asDouble(-1);
                if (ratio >= 0) scores.put(category, (int) Math.round(Math.min(100, ratio)));
            } catch (IllegalArgumentException ignored) {
                // 알 수 없는 그룹은 저장하지 않습니다.
            }
        }
        return scores;
    }

    private static Map<String, Object> group(NewsCategory category, String... keywords) {
        return Map.of("groupName", category.name(), "keywords", List.of(keywords));
    }
}
