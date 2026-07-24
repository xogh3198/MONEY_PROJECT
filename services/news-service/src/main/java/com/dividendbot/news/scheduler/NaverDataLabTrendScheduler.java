package com.dividendbot.news.scheduler;

import com.dividendbot.news.domain.entity.NewsArticle;
import com.dividendbot.news.domain.entity.NewsCategory;
import com.dividendbot.news.domain.repository.NewsArticleRepository;
import com.dividendbot.news.service.CollectorRunStateService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(name = "naver.datalab.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class NaverDataLabTrendScheduler {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final String ARTICLE_GROUP_PREFIX = "ARTICLE_";
    private static final String REFERENCE_GROUP = "REFERENCE";
    private static final int ARTICLE_BATCH_SIZE = 4;
    private static final int ARTICLE_CANDIDATES_PER_CATEGORY = 8;
    private static final int REFERENCE_SCORE = 50;
    private static final Pattern TITLE_TOKEN = Pattern.compile("[가-힣A-Za-z0-9]+");
    private static final Set<String> TITLE_STOP_WORDS = Set.of(
            "종합", "속보", "단독", "연합뉴스", "기자", "뉴스", "관련", "대한",
            "오늘", "올해", "지난", "이번", "전망", "발표", "가능성", "가운데"
    );
    private static final Map<String, Object> REFERENCE_KEYWORD_GROUP =
            Map.of("groupName", REFERENCE_GROUP, "keywords", List.of("코스피"));
    private static final List<Map<String, Object>> KEYWORD_GROUPS = List.of(
            group(NewsCategory.DOMESTIC, "코스피", "코스닥", "국내증시"),
            group(NewsCategory.OVERSEAS, "나스닥", "S&P500", "미국증시"),
            group(NewsCategory.FOREX, "원달러 환율", "달러 환율", "환율"),
            group(NewsCategory.RATE, "기준금리", "금리 인하", "FOMC"),
            group(NewsCategory.CRYPTO, "비트코인", "이더리움", "암호화폐")
    );

    private final NewsArticleRepository newsRepository;
    private final CollectorRunStateService runStateService;

    @Value("${naver.api.client-id:}")
    private String clientId;

    @Value("${naver.api.client-secret:}")
    private String clientSecret;

    @Scheduled(fixedDelay = 21_600_000, initialDelay = 60_000)
    public void refreshSearchInterest() {
        long startedAt = System.nanoTime();
        boolean configured = hasText(clientId) && hasText(clientSecret);
        if (!configured) {
            recordSafely(() -> runStateService.markSkipped(
                    CollectorRunStateService.NAVER_DATALAB,
                    true,
                    false,
                    "NAVER_CLIENT_ID 또는 NAVER_CLIENT_SECRET이 설정되지 않았습니다."
            ));
            return;
        }

        recordSafely(() -> runStateService.markRunning(
                CollectorRunStateService.NAVER_DATALAB,
                true,
                true
        ));

        try {
            LocalDate endDate = LocalDate.now(SEOUL).minusDays(1);
            JsonNode response = requestDataLab(endDate, KEYWORD_GROUPS);

            Map<NewsCategory, Integer> scores = readLatestScores(response);
            if (scores.isEmpty()) {
                recordSafely(() -> runStateService.markFailed(
                        CollectorRunStateService.NAVER_DATALAB,
                        elapsedMillis(startedAt),
                        "DataLab 응답에 사용할 수 있는 카테고리 점수가 없습니다."
                ));
                return;
            }

            LocalDateTime updatedAt = LocalDateTime.now();
            List<NewsArticle> recent = newsRepository.findByPublishedAtAfter(updatedAt.minusDays(7));
            for (NewsArticle article : recent) {
                Integer score = scores.get(article.getCategory());
                if (score != null) article.updateExternalSearchInterest(score, "NAVER_DATALAB_CATEGORY", updatedAt);
            }
            newsRepository.saveAll(recent);

            List<NewsArticle> candidates = findBalancedArticleCandidates(updatedAt.minusDays(2));
            int articleSpecificCount = updateArticleSpecificScores(candidates, endDate, updatedAt);
            recordSafely(() -> runStateService.markSuccess(
                    CollectorRunStateService.NAVER_DATALAB,
                    recent.size(),
                    articleSpecificCount,
                    elapsedMillis(startedAt),
                    "분야 " + scores.size() + "개 fallback과 기사별 점수 "
                            + articleSpecificCount + "/" + candidates.size() + "건을 반영했습니다."
            ));
            log.info(
                    "네이버 DataLab 검색 관심도 갱신: 분야 {}개, 기사별 {}/{}건, 전체 fallback {}건",
                    scores.size(),
                    articleSpecificCount,
                    candidates.size(),
                    recent.size()
            );
        } catch (WebClientResponseException e) {
            String message = "NAVER DataLab HTTP " + e.getStatusCode().value()
                    + ". 개발자센터의 DataLab 권한과 호출 한도를 확인하세요.";
            recordSafely(() -> runStateService.markFailed(
                    CollectorRunStateService.NAVER_DATALAB,
                    elapsedMillis(startedAt),
                    message
            ));
            log.warn("네이버 DataLab 검색 관심도 갱신 실패: {}", message);
        } catch (Exception e) {
            recordSafely(() -> runStateService.markFailed(
                    CollectorRunStateService.NAVER_DATALAB,
                    elapsedMillis(startedAt),
                    e.getClass().getSimpleName() + ": " + safeMessage(e)
            ));
            log.warn("네이버 DataLab 검색 관심도 갱신 실패. API 권한과 호출 한도를 확인하세요: {}", e.getMessage());
        }
    }

    List<NewsArticle> findBalancedArticleCandidates(LocalDateTime since) {
        List<NewsArticle> candidates = new ArrayList<>();
        PageRequest perCategory = PageRequest.of(0, ARTICLE_CANDIDATES_PER_CATEGORY);
        for (NewsCategory category : NewsCategory.values()) {
            candidates.addAll(newsRepository.findByCategoryAndPublishedAtAfterOrderByPublishedAtDesc(
                    category,
                    since,
                    perCategory
            ));
        }
        return candidates;
    }

    private int updateArticleSpecificScores(
            List<NewsArticle> candidates,
            LocalDate endDate,
            LocalDateTime updatedAt
    ) {
        int updated = 0;
        for (int start = 0; start < candidates.size(); start += ARTICLE_BATCH_SIZE) {
            List<NewsArticle> batch = candidates.subList(
                    start,
                    Math.min(start + ARTICLE_BATCH_SIZE, candidates.size())
            );
            List<Map<String, Object>> groups = new ArrayList<>();
            groups.add(REFERENCE_KEYWORD_GROUP);
            Map<String, NewsArticle> articlesByGroup = new LinkedHashMap<>();

            for (int index = 0; index < batch.size(); index++) {
                NewsArticle article = batch.get(index);
                List<String> keywords = extractTitleKeywords(article.getTitle());
                if (keywords.isEmpty()) continue;
                String groupName = ARTICLE_GROUP_PREFIX + index;
                groups.add(Map.of("groupName", groupName, "keywords", keywords));
                articlesByGroup.put(groupName, article);
            }
            if (articlesByGroup.isEmpty()) continue;

            try {
                Map<String, Integer> articleScores = readArticleScores(requestDataLab(endDate, groups));
                for (Map.Entry<String, Integer> entry : articleScores.entrySet()) {
                    NewsArticle article = articlesByGroup.get(entry.getKey());
                    if (article == null) continue;
                    article.updateExternalSearchInterest(
                            entry.getValue(),
                            "NAVER_DATALAB_ARTICLE_KEYWORDS",
                            updatedAt
                    );
                    updated++;
                }
            } catch (Exception batchError) {
                log.warn(
                        "DataLab 기사별 관심도 배치 건너뜀({}~{}): {}",
                        start,
                        start + batch.size() - 1,
                        safeMessage(batchError)
                );
            }
        }
        newsRepository.saveAll(candidates);
        return updated;
    }

    private JsonNode requestDataLab(LocalDate endDate, List<Map<String, Object>> keywordGroups) {
        Map<String, Object> request = Map.of(
                "startDate", endDate.minusDays(29).toString(),
                "endDate", endDate.toString(),
                "timeUnit", "date",
                "keywordGroups", keywordGroups
        );
        return WebClient.builder()
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
    }

    Map<NewsCategory, Integer> readLatestScores(JsonNode response) {
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

    Map<String, Integer> readArticleScores(JsonNode response) {
        Map<String, Integer> scores = new LinkedHashMap<>();
        if (response == null || !response.path("results").isArray()) return scores;

        Map<String, Double> averages = new LinkedHashMap<>();
        for (JsonNode result : response.path("results")) {
            double average = averageRatio(result.path("data"));
            if (average >= 0) averages.put(result.path("title").asText(), average);
        }
        double reference = averages.getOrDefault(REFERENCE_GROUP, -1.0);
        if (reference <= 0) return scores;

        for (Map.Entry<String, Double> entry : averages.entrySet()) {
            if (!entry.getKey().startsWith(ARTICLE_GROUP_PREFIX)) continue;
            int score = (int) Math.round(entry.getValue() / reference * REFERENCE_SCORE);
            scores.put(entry.getKey(), Math.max(0, Math.min(100, score)));
        }
        return scores;
    }

    List<String> extractTitleKeywords(String title) {
        if (title == null || title.isBlank()) return List.of();
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        Matcher matcher = TITLE_TOKEN.matcher(title);
        while (matcher.find() && tokens.size() < 4) {
            String token = matcher.group();
            if (token.length() < 2 || token.chars().allMatch(Character::isDigit)) continue;
            if (TITLE_STOP_WORDS.contains(token)) continue;
            tokens.add(token);
        }
        if (tokens.isEmpty()) return List.of();

        List<String> tokenList = new ArrayList<>(tokens);
        List<String> keywords = new ArrayList<>();
        if (tokenList.size() >= 2) keywords.add(tokenList.get(0) + " " + tokenList.get(1));
        keywords.addAll(tokenList);
        return keywords.stream().limit(5).toList();
    }

    private double averageRatio(JsonNode data) {
        if (!data.isArray() || data.isEmpty()) return -1;
        double sum = 0;
        int count = 0;
        for (JsonNode point : data) {
            double ratio = point.path("ratio").asDouble(-1);
            if (ratio < 0) continue;
            sum += ratio;
            count++;
        }
        return count == 0 ? -1 : sum / count;
    }

    private static Map<String, Object> group(NewsCategory category, String... keywords) {
        return Map.of("groupName", category.name(), "keywords", List.of(keywords));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private String safeMessage(Exception error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? "상세 오류 없음" : message;
    }

    private void recordSafely(Runnable action) {
        try {
            action.run();
        } catch (Exception statusError) {
            log.warn("DataLab 실행 상태 저장 실패: {}", statusError.getMessage());
        }
    }
}
