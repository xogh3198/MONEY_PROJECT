package com.dividendbot.news.scheduler;

import com.dividendbot.news.domain.entity.NewsArticle;
import com.dividendbot.news.domain.entity.NewsCategory;
import com.dividendbot.news.domain.entity.NewsSentiment;
import com.dividendbot.news.domain.repository.NewsArticleRepository;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * 뉴스 수집 크롤러
 * 
 * 전략:
 * 1차: 네이버 뉴스 검색 API (카테고리별 키워드 검색, 하루 25,000건 가능)
 * 2차: RSS 보조 수집 (한경, 매경, 연합뉴스)
 * 
 * 실행: 15분 간격
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NewsRssCrawler {

    private final NewsArticleRepository newsRepository;

    @Value("${naver.api.client-id:}")
    private String naverClientId;

    @Value("${naver.api.client-secret:}")
    private String naverClientSecret;

    // 카테고리별 검색 키워드
    private static final Map<NewsCategory, List<String>> SEARCH_KEYWORDS = Map.of(
            NewsCategory.DOMESTIC, List.of("코스피 증시", "코스닥 주가", "국내증시"),
            NewsCategory.OVERSEAS, List.of("나스닥", "미국증시 S&P500", "해외증시"),
            NewsCategory.FOREX, List.of("원달러 환율", "달러 강세", "환율 전망"),
            NewsCategory.RATE, List.of("기준금리", "한국은행 금리", "금리 인하"),
            NewsCategory.CRYPTO, List.of("비트코인", "암호화폐 시장", "이더리움")
    );

    // RSS 보조 소스
    private static final List<RssSource> RSS_SOURCES = List.of(
            new RssSource("한국경제", "https://www.hankyung.com/feed/economy", NewsCategory.DOMESTIC),
            new RssSource("매일경제", "https://www.mk.co.kr/rss/30100041/", NewsCategory.DOMESTIC),
            new RssSource("연합뉴스", "https://www.yna.co.kr/rss/economy.xml", NewsCategory.DOMESTIC)
    );

    /**
     * 15분 간격 뉴스 수집 (메인 스케줄러)
     */
    @Scheduled(fixedRate = 900000)
    public void crawlNews() {
        log.info("=== 뉴스 수집 시작 ===");
        int total = 0;

        // 1차: 네이버 API
        if (!naverClientId.isBlank()) {
            total += crawlFromNaverApi();
        } else {
            log.warn("네이버 API 키 미설정. RSS만 수집합니다. (NAVER_CLIENT_ID 환경변수 필요)");
        }

        // 2차: RSS 보조
        total += crawlFromRss();

        log.info("=== 뉴스 수집 완료: 신규 {}건 ===", total);
    }

    /**
     * 네이버 뉴스 검색 API 수집
     */
    private int crawlFromNaverApi() {
        WebClient client = WebClient.builder()
                .baseUrl("https://openapi.naver.com")
                .defaultHeader("X-Naver-Client-Id", naverClientId)
                .defaultHeader("X-Naver-Client-Secret", naverClientSecret)
                .build();

        int saved = 0;

        for (Map.Entry<NewsCategory, List<String>> entry : SEARCH_KEYWORDS.entrySet()) {
            NewsCategory category = entry.getKey();
            // 카테고리당 첫 번째 키워드로 검색
            String keyword = entry.getValue().get(0);

            try {
                Map response = client.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/v1/search/news.json")
                                .queryParam("query", keyword)
                                .queryParam("display", 10)
                                .queryParam("sort", "date")
                                .build())
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

                if (response == null) continue;

                List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
                if (items == null) continue;

                for (Map<String, Object> item : items) {
                    String title = cleanHtml((String) item.getOrDefault("title", ""));
                    String description = cleanHtml((String) item.getOrDefault("description", ""));
                    String link = (String) item.getOrDefault("originallink", item.get("link"));

                    if (title.isEmpty() || newsRepository.existsBySourceUrl(link)) continue;

                    NewsArticle article = NewsArticle.builder()
                            .title(title)
                            .summary(description.length() > 200 ? description.substring(0, 200) + "..." : description)
                            .sourceUrl(link)
                            .sourceName(extractSource(link))
                            .category(category)
                            .sentiment(analyzeSentiment(title + " " + description))
                            .publishedAt(LocalDateTime.now())
                            .build();

                    newsRepository.save(article);
                    saved++;
                }

                // Rate limit 준수 (네이버 API)
                Thread.sleep(100);
            } catch (Exception e) {
                log.warn("네이버 API 수집 실패 [{}]: {}", keyword, e.getMessage());
            }
        }

        log.debug("네이버 API: {}건 저장", saved);
        return saved;
    }

    /**
     * RSS 보조 수집
     */
    private int crawlFromRss() {
        int saved = 0;

        for (RssSource source : RSS_SOURCES) {
            try {
                SyndFeedInput input = new SyndFeedInput();
                SyndFeed feed = input.build(new XmlReader(URI.create(source.url).toURL()));

                for (SyndEntry entry : feed.getEntries()) {
                    String title = entry.getTitle() != null ? entry.getTitle().trim() : "";
                    if (title.isEmpty()) continue;

                    String link = entry.getLink() != null ? entry.getLink() : "";
                    if (newsRepository.existsBySourceUrl(link) || newsRepository.existsByTitle(title)) continue;

                    String description = entry.getDescription() != null
                            ? entry.getDescription().getValue().replaceAll("<[^>]*>", "").trim()
                            : "";

                    LocalDateTime publishedAt = entry.getPublishedDate() != null
                            ? entry.getPublishedDate().toInstant().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime()
                            : LocalDateTime.now();

                    NewsArticle article = NewsArticle.builder()
                            .title(title)
                            .summary(description.length() > 200 ? description.substring(0, 200) + "..." : description)
                            .sourceUrl(link)
                            .sourceName(source.name)
                            .category(classifyCategory(title + " " + description, source.defaultCategory))
                            .sentiment(analyzeSentiment(title + " " + description))
                            .publishedAt(publishedAt)
                            .build();

                    newsRepository.save(article);
                    saved++;
                }
            } catch (Exception e) {
                log.debug("RSS 수집 실패 [{}]: {}", source.name, e.getMessage());
            }
        }

        log.debug("RSS: {}건 저장", saved);
        return saved;
    }

    private String cleanHtml(String text) {
        return text.replaceAll("<[^>]*>", "").replaceAll("&quot;", "\"")
                .replaceAll("&amp;", "&").replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">").trim();
    }

    private String extractSource(String url) {
        if (url == null) return "기타";
        if (url.contains("hankyung")) return "한국경제";
        if (url.contains("mk.co.kr")) return "매일경제";
        if (url.contains("yna.co.kr")) return "연합뉴스";
        if (url.contains("chosun")) return "조선일보";
        if (url.contains("donga")) return "동아일보";
        if (url.contains("sedaily")) return "서울경제";
        return "기타";
    }

    private NewsCategory classifyCategory(String text, NewsCategory defaultCat) {
        String lower = text.toLowerCase();
        if (lower.contains("환율") || lower.contains("달러") || lower.contains("원화")) return NewsCategory.FOREX;
        if (lower.contains("금리") || lower.contains("기준금리") || lower.contains("인하")) return NewsCategory.RATE;
        if (lower.contains("비트코인") || lower.contains("암호화폐") || lower.contains("코인")) return NewsCategory.CRYPTO;
        if (lower.contains("나스닥") || lower.contains("s&p") || lower.contains("다우") || lower.contains("미국")) return NewsCategory.OVERSEAS;
        return defaultCat;
    }

    private NewsSentiment analyzeSentiment(String text) {
        String lower = text.toLowerCase();
        int pos = 0, neg = 0;
        for (String w : new String[]{"상승", "호조", "서프라이즈", "최고", "돌파", "순매수", "반등", "회복", "신고가", "강세", "호실적"}) {
            if (lower.contains(w)) pos++;
        }
        for (String w : new String[]{"하락", "위기", "급락", "폭락", "매도", "불안", "둔화", "침체", "약세", "리스크", "우려"}) {
            if (lower.contains(w)) neg++;
        }
        if (pos > neg) return NewsSentiment.POSITIVE;
        if (neg > pos) return NewsSentiment.NEGATIVE;
        return NewsSentiment.NEUTRAL;
    }

    private record RssSource(String name, String url, NewsCategory defaultCategory) {}
}
