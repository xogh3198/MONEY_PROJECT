package com.dividendbot.news.scheduler;

import com.dividendbot.news.domain.entity.NewsArticle;
import com.dividendbot.news.domain.entity.NewsCategory;
import com.dividendbot.news.domain.entity.NewsSentiment;
import com.dividendbot.news.domain.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 경제 뉴스 RSS 자동 수집 스케줄러
 * - 15분 간격 실행
 * - 한경, 매경, 연합뉴스 경제 RSS 수집
 * - AI 감성 분석 (MVP: 키워드 기반 간이 분류)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NewsRssCrawler {

    private final NewsArticleRepository newsRepository;
    private final WebClient webClient = WebClient.create();

    // RSS 소스 목록
    private static final List<RssSource> RSS_SOURCES = List.of(
            new RssSource("한국경제", "https://www.hankyung.com/feed/economy", NewsCategory.DOMESTIC),
            new RssSource("매일경제", "https://www.mk.co.kr/rss/30100041/", NewsCategory.DOMESTIC),
            new RssSource("연합뉴스 경제", "https://www.yna.co.kr/economy/rss", NewsCategory.DOMESTIC)
    );

    /**
     * 15분 간격 뉴스 수집
     */
    @Scheduled(fixedRate = 900000) // 15분 = 900,000ms
    public void crawlNews() {
        log.info("=== 뉴스 수집 시작 ===");
        int collected = 0;

        for (RssSource source : RSS_SOURCES) {
            try {
                List<NewsArticle> articles = fetchFromRss(source);
                for (NewsArticle article : articles) {
                    // 중복 체크 (제목 기반)
                    if (!newsRepository.findTop10ByOrderByViewCountDesc().stream()
                            .anyMatch(existing -> existing.getTitle().equals(article.getTitle()))) {
                        newsRepository.save(article);
                        collected++;
                    }
                }
            } catch (Exception e) {
                log.error("RSS 수집 실패: {} - {}", source.name, e.getMessage());
            }
        }

        log.info("=== 뉴스 수집 완료: {}건 추가 ===", collected);
    }

    private List<NewsArticle> fetchFromRss(RssSource source) {
        // MVP: RSS 파싱 대신 시드 데이터 반환 (실제 RSS 파싱은 Phase 7에서 XML 파서 추가)
        // TODO: Rome 또는 JDOM 라이브러리로 RSS XML 실제 파싱
        log.debug("Fetching RSS: {}", source.url);

        // 시드 뉴스 생성 (RSS 연동 전까지 데모용)
        return List.of(
                NewsArticle.builder()
                        .title(generateDemoTitle(source.category))
                        .summary("AI가 생성한 3줄 요약이 여기에 표시됩니다.")
                        .sourceUrl(source.url)
                        .sourceName(source.name)
                        .category(source.category)
                        .sentiment(analyzeSentiment(""))
                        .publishedAt(LocalDateTime.now())
                        .build()
        );
    }

    /**
     * MVP 감성 분석 (키워드 기반 간이 분류)
     * TODO: Phase 7에서 실제 NLP/LLM 기반 분석으로 교체
     */
    private NewsSentiment analyzeSentiment(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("상승") || lower.contains("호조") || lower.contains("서프라이즈")) {
            return NewsSentiment.POSITIVE;
        }
        if (lower.contains("하락") || lower.contains("위기") || lower.contains("급락")) {
            return NewsSentiment.NEGATIVE;
        }
        return NewsSentiment.NEUTRAL;
    }

    private String generateDemoTitle(NewsCategory category) {
        return switch (category) {
            case DOMESTIC -> "국내 증시 동향: 코스피 " + (Math.random() > 0.5 ? "상승" : "하락") + " 마감";
            case OVERSEAS -> "미국 증시: 나스닥 " + (Math.random() > 0.5 ? "신고가" : "조정");
            case FOREX -> "원/달러 환율 " + (1330 + (int)(Math.random() * 30)) + "원대";
            case RATE -> "기준금리 동결, 하반기 전망은?";
            case CRYPTO -> "비트코인 $" + (95000 + (int)(Math.random() * 10000));
        };
    }

    private record RssSource(String name, String url, NewsCategory category) {}
}
