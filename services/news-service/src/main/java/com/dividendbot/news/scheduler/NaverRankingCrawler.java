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
 * DB에 있는 기사의 인기도(viewCount)를 업데이트하거나,
 * 없는 기사는 높은 초기 인기도와 함께 저장합니다.
 *
 * 이를 통해 사이트 초반에도 실제 인기 기사가 상위에 노출됩니다.
 *
 * 실행: 30분 간격
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NaverRankingCrawler {

    private final NewsArticleRepository newsRepository;

    private static final String RANKING_URL = "https://news.naver.com/main/ranking/popularDay.naver";
    private static final int[] RANK_SCORES = {100, 80, 60, 45, 30}; // 1위~5위 점수

    /**
     * 30분 간격으로 네이버 랭킹뉴스 크롤링
     */
    @Scheduled(fixedRate = 1800000, initialDelay = 30000)
    @Transactional
    public void crawlRankingNews() {
        log.info("=== 네이버 랭킹뉴스 수집 시작 ===");
        int updated = 0;
        int created = 0;

        try {
            Document doc = Jsoup.connect(RANKING_URL)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .get();

            // 각 언론사별 랭킹 박스에서 기사 추출
            Elements rankingItems = doc.select("a[href*=n.news.naver.com/article]");

            Map<String, Integer> articleScores = new HashMap<>();

            for (Element link : rankingItems) {
                String url = link.attr("href");
                String title = link.text().trim();

                // 순위 추출: 부모 요소에서 순위 정보 파악
                Element parent = link.parent();
                int rank = extractRank(parent, link);
                int score = (rank >= 1 && rank <= 5) ? RANK_SCORES[rank - 1] : 20;

                if (!title.isEmpty() && !url.isEmpty()) {
                    // 같은 URL이 여러 번 나올 수 있으므로 최대 점수 유지
                    articleScores.merge(url + "|||" + title, score, Math::max);
                }
            }

            for (Map.Entry<String, Integer> entry : articleScores.entrySet()) {
                String[] parts = entry.getKey().split("\\|\\|\\|", 2);
                String url = parts[0];
                String title = parts.length > 1 ? parts[1] : "";
                int score = entry.getValue();

                try {
                    // 이미 DB에 있는 기사인지 확인 (URL 또는 제목 매칭)
                    Optional<NewsArticle> existing = Optional.empty();
                    if (newsRepository.existsBySourceUrl(url)) {
                        existing = newsRepository.findBySourceUrl(url);
                    } else if (!title.isEmpty() && newsRepository.existsByTitle(title)) {
                        existing = newsRepository.findFirstByTitle(title);
                    }

                    if (existing.isPresent()) {
                        // 기존 기사: 인기도 가중치 추가
                        NewsArticle article = existing.get();
                        article.addRankingBoost(score);
                        newsRepository.save(article);
                        updated++;
                    } else if (!title.isEmpty()) {
                        // 새 기사: 높은 초기 인기도로 저장
                        NewsArticle article = NewsArticle.builder()
                                .title(title)
                                .summary("")
                                .sourceUrl(url)
                                .sourceName(extractSourceFromNaverUrl(url))
                                .category(classifyCategory(title))
                                .sentiment(analyzeSentiment(title))
                                .viewCount(score)
                                .publishedAt(LocalDateTime.now())
                                .build();
                        newsRepository.save(article);
                        created++;
                    }
                } catch (Exception e) {
                    log.debug("랭킹 기사 처리 실패: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            log.warn("네이버 랭킹뉴스 크롤링 실패: {}", e.getMessage());
        }

        log.info("=== 네이버 랭킹뉴스 수집 완료: 업데이트 {}건, 신규 {}건 ===", updated, created);
    }

    /**
     * 순위 추출 - 텍스트에서 숫자 또는 위치 기반
     */
    private int extractRank(Element parent, Element link) {
        if (parent == null) return 0;

        // 텍스트에서 "N위" 패턴 찾기
        String parentText = parent.text();
        for (int i = 1; i <= 5; i++) {
            if (parentText.startsWith(i + "위")) return i;
        }

        // 형제 요소 중 순서로 추정
        Elements siblings = parent.parent() != null ? parent.parent().children() : new Elements();
        int index = siblings.indexOf(parent);
        if (index >= 0 && index < 5) return index + 1;

        return 0;
    }

    private String extractSourceFromNaverUrl(String url) {
        // n.news.naver.com/article/018/... → 언론사 코드로 매칭
        try {
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
        if (lower.contains("환율") || lower.contains("달러") || lower.contains("원화")) return NewsCategory.FOREX;
        if (lower.contains("금리") || lower.contains("기준금리")) return NewsCategory.RATE;
        if (lower.contains("비트코인") || lower.contains("암호화폐") || lower.contains("코인")) return NewsCategory.CRYPTO;
        if (lower.contains("나스닥") || lower.contains("s&p") || lower.contains("다우") || lower.contains("미국증시")) return NewsCategory.OVERSEAS;
        if (lower.contains("코스피") || lower.contains("코스닥") || lower.contains("주가") || lower.contains("증시")
                || lower.contains("삼성") || lower.contains("반도체") || lower.contains("주식")) return NewsCategory.DOMESTIC;
        return NewsCategory.DOMESTIC; // 경제 뉴스 사이트이므로 기본값
    }

    private NewsSentiment analyzeSentiment(String text) {
        String lower = text.toLowerCase();
        int pos = 0, neg = 0;
        for (String w : new String[]{"상승", "호조", "최고", "돌파", "반등", "회복", "강세", "랠리"}) {
            if (lower.contains(w)) pos++;
        }
        for (String w : new String[]{"하락", "위기", "급락", "폭락", "불안", "침체", "약세", "우려"}) {
            if (lower.contains(w)) neg++;
        }
        if (pos > neg) return NewsSentiment.POSITIVE;
        if (neg > pos) return NewsSentiment.NEGATIVE;
        return NewsSentiment.NEUTRAL;
    }
}
