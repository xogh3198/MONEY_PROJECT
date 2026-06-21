package com.dividendbot.news.scheduler;

import com.dividendbot.news.domain.entity.NewsArticle;
import com.dividendbot.news.domain.entity.NewsCategory;
import com.dividendbot.news.domain.entity.NewsSentiment;
import com.dividendbot.news.domain.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 네이버 랭킹뉴스 크롤러
 *
 * 네이버 "많이 본 뉴스" 랭킹 페이지를 크롤링하여
 * 인기도(viewCount) 높은 기사를 DB에 저장합니다.
 *
 * 기존 기사 매칭보다는 랭킹 기사 자체를 높은 viewCount로 저장하여
 * 인기뉴스 알고리즘에 자연스럽게 반영되도록 합니다.
 *
 * 실행: 30분 간격
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NaverRankingCrawler {

    private final NewsArticleRepository newsRepository;

    private static final String RANKING_URL = "https://news.naver.com/main/ranking/popularDay.naver";
    // 1위~5위 인기도 점수 (viewCount에 반영)
    private static final int[] RANK_SCORES = {150, 120, 90, 70, 50};

    /**
     * 30분 간격으로 네이버 랭킹뉴스 크롤링
     */
    @Scheduled(fixedRate = 1800000, initialDelay = 10000)
    @Transactional
    public void crawlRankingNews() {
        log.info("=== 네이버 랭킹뉴스 수집 시작 ===");
        int saved = 0;
        int boosted = 0;

        try {
            Document doc = Jsoup.connect(RANKING_URL)
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml")
                    .header("Accept-Language", "ko-KR,ko;q=0.9")
                    .timeout(15000)
                    .get();

            // 네이버 랭킹 페이지에서 기사 링크 추출
            // 패턴: n.news.naver.com/article/OID/AID
            Elements links = doc.select("a[href*=n.news.naver.com/article]");

            // 순서대로 처리 - 먼저 나오는 것이 상위 랭킹
            int rankCounter = 0;
            int pressCounter = 0;
            String lastPress = "";

            for (Element link : links) {
                String url = link.attr("href");
                if (!url.startsWith("http")) {
                    url = "https:" + url;
                }

                String title = link.text().trim();
                if (title.isEmpty() || title.length() < 5) continue;

                // 언론사별로 1~5위가 반복됨
                String currentPress = extractPressFromUrl(url);
                if (!currentPress.equals(lastPress)) {
                    rankCounter = 0;
                    lastPress = currentPress;
                    pressCounter++;
                }
                rankCounter++;

                // 각 언론사 최대 5개까지만
                if (rankCounter > 5) continue;

                int score = (rankCounter <= 5) ? RANK_SCORES[rankCounter - 1] : 30;
                // 상위 언론사일수록 약간 더 높은 점수
                if (pressCounter <= 3) score += 20;

                // 이미 DB에 있는지 확인 (URL 기준)
                if (newsRepository.existsBySourceUrl(url)) {
                    // 기존 기사 인기도 부스트
                    Optional<NewsArticle> existing = newsRepository.findBySourceUrl(url);
                    if (existing.isPresent()) {
                        existing.get().addRankingBoost(score);
                        newsRepository.save(existing.get());
                        boosted++;
                    }
                    continue;
                }

                // 제목으로 중복 확인
                if (newsRepository.existsByTitle(title)) {
                    Optional<NewsArticle> existing = newsRepository.findFirstByTitle(title);
                    if (existing.isPresent()) {
                        existing.get().addRankingBoost(score);
                        newsRepository.save(existing.get());
                        boosted++;
                    }
                    continue;
                }

                // 새 기사 저장 - 높은 초기 viewCount
                NewsArticle article = NewsArticle.builder()
                        .title(title)
                        .summary("")
                        .sourceUrl(url)
                        .sourceName(currentPress)
                        .category(classifyCategory(title))
                        .sentiment(analyzeSentiment(title))
                        .viewCount(score)
                        .publishedAt(LocalDateTime.now())
                        .build();

                newsRepository.save(article);
                saved++;
            }

        } catch (Exception e) {
            log.error("네이버 랭킹뉴스 크롤링 실패: {}", e.getMessage(), e);
        }

        log.info("=== 네이버 랭킹뉴스 수집 완료: 신규 {}건, 부스트 {}건 ===", saved, boosted);
    }

    private String extractPressFromUrl(String url) {
        try {
            // https://n.news.naver.com/article/018/0006311260
            String[] parts = url.split("/article/");
            if (parts.length > 1) {
                String oid = parts[1].split("/")[0];
                return getPressByOid(oid);
            }
        } catch (Exception ignored) {}
        return "기타";
    }

    private String getPressByOid(String oid) {
        return switch (oid) {
            case "421" -> "뉴스1";
            case "018" -> "이데일리";
            case "025" -> "중앙일보";
            case "366" -> "조선비즈";
            case "056" -> "KBS";
            case "011" -> "서울경제";
            case "055" -> "SBS";
            case "015" -> "한국경제";
            case "437" -> "JTBC";
            case "008" -> "머니투데이";
            case "023" -> "조선일보";
            case "215" -> "한국경제TV";
            case "009" -> "매일경제";
            case "014" -> "파이낸셜뉴스";
            case "016" -> "헤럴드경제";
            case "020" -> "동아일보";
            case "028" -> "한겨레";
            case "032" -> "경향신문";
            case "052" -> "YTN";
            case "081" -> "서울신문";
            case "214" -> "MBC";
            case "422" -> "연합뉴스TV";
            case "001" -> "연합뉴스";
            default -> "기타";
        };
    }

    private NewsCategory classifyCategory(String title) {
        String lower = title.toLowerCase();
        if (lower.contains("환율") || lower.contains("달러") || lower.contains("원화") || lower.contains("엔화")) return NewsCategory.FOREX;
        if (lower.contains("금리") || lower.contains("기준금리") || lower.contains("한은")) return NewsCategory.RATE;
        if (lower.contains("비트코인") || lower.contains("암호화폐") || lower.contains("코인") || lower.contains("이더리움")) return NewsCategory.CRYPTO;
        if (lower.contains("나스닥") || lower.contains("s&p") || lower.contains("다우") || lower.contains("미국증시") || lower.contains("월가")) return NewsCategory.OVERSEAS;
        if (lower.contains("코스피") || lower.contains("코스닥") || lower.contains("주가") || lower.contains("증시")
                || lower.contains("삼성") || lower.contains("반도체") || lower.contains("주식") || lower.contains("상장")) return NewsCategory.DOMESTIC;
        return NewsCategory.DOMESTIC;
    }

    private NewsSentiment analyzeSentiment(String text) {
        String lower = text.toLowerCase();
        int pos = 0, neg = 0;
        for (String w : new String[]{"상승", "호조", "최고", "돌파", "반등", "회복", "강세", "랠리", "신고가", "흑자"}) {
            if (lower.contains(w)) pos++;
        }
        for (String w : new String[]{"하락", "위기", "급락", "폭락", "불안", "침체", "약세", "우려", "적자", "손실"}) {
            if (lower.contains(w)) neg++;
        }
        if (pos > neg) return NewsSentiment.POSITIVE;
        if (neg > pos) return NewsSentiment.NEGATIVE;
        return NewsSentiment.NEUTRAL;
    }
}
