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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 경제 뉴스 RSS 실제 파싱 크롤러
 * - Rome 라이브러리로 RSS XML 파싱
 * - 15분 간격 자동 수집
 * - 키워드 기반 감성 분석 + 카테고리 재분류
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NewsRssCrawler {

    private final NewsArticleRepository newsRepository;

    private static final List<RssSource> RSS_SOURCES = List.of(
            new RssSource("한국경제", "https://www.hankyung.com/feed/economy", NewsCategory.DOMESTIC),
            new RssSource("매일경제", "https://www.mk.co.kr/rss/30100041/", NewsCategory.DOMESTIC),
            new RssSource("연합뉴스", "https://www.yna.co.kr/rss/economy.xml", NewsCategory.DOMESTIC)
    );

    @Scheduled(fixedRate = 900000) // 15분
    public void crawlNews() {
        log.info("=== RSS 뉴스 수집 시작 ===");
        int total = 0;

        for (RssSource source : RSS_SOURCES) {
            try {
                List<NewsArticle> articles = parseRssFeed(source);
                int saved = 0;
                for (NewsArticle article : articles) {
                    // 제목 중복 체크
                    boolean exists = newsRepository.existsByTitle(article.getTitle());
                    if (!exists) {
                        newsRepository.save(article);
                        saved++;
                    }
                }
                total += saved;
                log.debug("{}: {}건 수집, {}건 신규 저장", source.name, articles.size(), saved);
            } catch (Exception e) {
                log.warn("RSS 파싱 실패 [{}]: {}", source.name, e.getMessage());
            }
        }

        log.info("=== RSS 수집 완료: 신규 {}건 ===", total);
    }

    private List<NewsArticle> parseRssFeed(RssSource source) {
        List<NewsArticle> results = new ArrayList<>();

        try {
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(URI.create(source.url).toURL()));

            for (SyndEntry entry : feed.getEntries()) {
                String title = entry.getTitle() != null ? entry.getTitle().trim() : "";
                if (title.isEmpty()) continue;

                String description = entry.getDescription() != null
                        ? entry.getDescription().getValue().replaceAll("<[^>]*>", "").trim()
                        : "";
                // 3줄 요약 (간이: 첫 150자)
                String summary = description.length() > 150
                        ? description.substring(0, 150) + "..."
                        : description;

                LocalDateTime publishedAt = entry.getPublishedDate() != null
                        ? entry.getPublishedDate().toInstant().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime()
                        : LocalDateTime.now();

                String link = entry.getLink() != null ? entry.getLink() : "";

                NewsArticle article = NewsArticle.builder()
                        .title(title)
                        .summary(summary)
                        .sourceUrl(link)
                        .sourceName(source.name)
                        .category(classifyCategory(title + " " + description, source.defaultCategory))
                        .sentiment(analyzeSentiment(title + " " + description))
                        .publishedAt(publishedAt)
                        .build();

                results.add(article);
            }
        } catch (Exception e) {
            log.error("Feed 파싱 에러 [{}]: {}", source.url, e.getMessage());
        }

        return results;
    }

    /**
     * 키워드 기반 카테고리 재분류
     */
    private NewsCategory classifyCategory(String text, NewsCategory defaultCat) {
        String lower = text.toLowerCase();
        if (lower.contains("환율") || lower.contains("달러") || lower.contains("원화")) return NewsCategory.FOREX;
        if (lower.contains("금리") || lower.contains("기준금리") || lower.contains("인하")) return NewsCategory.RATE;
        if (lower.contains("비트코인") || lower.contains("암호화폐") || lower.contains("이더리움")) return NewsCategory.CRYPTO;
        if (lower.contains("나스닥") || lower.contains("s&p") || lower.contains("다우")) return NewsCategory.OVERSEAS;
        return defaultCat;
    }

    /**
     * 키워드 기반 감성 분석
     */
    private NewsSentiment analyzeSentiment(String text) {
        String lower = text.toLowerCase();
        int positiveScore = 0;
        int negativeScore = 0;

        String[] positiveWords = {"상승", "호조", "서프라이즈", "최고", "돌파", "순매수", "반등", "회복", "신고가", "강세"};
        String[] negativeWords = {"하락", "위기", "급락", "폭락", "매도", "불안", "둔화", "침체", "약세", "리스크"};

        for (String word : positiveWords) if (lower.contains(word)) positiveScore++;
        for (String word : negativeWords) if (lower.contains(word)) negativeScore++;

        if (positiveScore > negativeScore) return NewsSentiment.POSITIVE;
        if (negativeScore > positiveScore) return NewsSentiment.NEGATIVE;
        return NewsSentiment.NEUTRAL;
    }

    private record RssSource(String name, String url, NewsCategory defaultCategory) {}
}
