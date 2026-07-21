package com.dividendbot.news.config;

import com.dividendbot.news.domain.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 앱 시작 시 더미 데이터 정리.
 * 이전 시드 데이터(example.com URL)가 있으면 삭제.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final NewsArticleRepository newsRepository;

    @Override
    public void run(String... args) {
        // 더미 데이터 정리 (example.com URL을 가진 시드 기사 삭제)
        long deleted = newsRepository.findAll().stream()
                .filter(a -> a.getSourceUrl() != null && a.getSourceUrl().contains("example.com"))
                .peek(a -> newsRepository.delete(a))
                .count();

        if (deleted > 0) {
            log.info("더미 뉴스 데이터 {}건 삭제 완료", deleted);
        }

        migrateLegacyRankingData();
    }

    /**
     * 과거 버전은 네이버 랭킹 점수를 실제 조회수에 누적했습니다.
     * 첫 실행에서 금융 기사 점수는 별도 필드로 옮기고, 참여가 없는 비금융 기사는 제거합니다.
     */
    private void migrateLegacyRankingData() {
        long migrated = 0;
        long removed = 0;

        for (var article : newsRepository.findAll()) {
            String url = article.getSourceUrl();
            if (url == null || !url.contains("ntype=RANKING")) continue;

            boolean hasEngagement = article.getCommentCount() > 0
                    || article.getPositiveVotes() > 0
                    || article.getNegativeVotes() > 0;

            if (!isFinanciallyRelevant(article.getTitle()) && !hasEngagement) {
                newsRepository.delete(article);
                removed++;
                continue;
            }

            Integer trendScore = article.getExternalTrendScore();
            if ((trendScore == null || trendScore == 0) && article.getViewCount() > 0 && !hasEngagement) {
                article.migrateLegacyRankingViews();
                newsRepository.save(article);
                migrated++;
            }
        }

        if (migrated > 0 || removed > 0) {
            log.info("랭킹 데이터 정리 완료: 조회수 분리 {}건, 비금융 제거 {}건", migrated, removed);
        }
    }

    private boolean isFinanciallyRelevant(String title) {
        if (title == null) return false;
        String lower = title.toLowerCase();
        return List.of(
                "주가", "증시", "코스피", "코스닥", "주식", "투자", "펀드", "etf",
                "금리", "환율", "달러", "원화", "엔화", "채권", "금융", "은행", "보험",
                "집값", "부동산", "청약", "전세", "대출", "반도체", "삼성전자", "하이닉스",
                "현대차", "수출", "무역", "물가", "경제", "실적", "매출", "영업이익",
                "비트코인", "암호화폐", "코인", "이더리움"
        ).stream().anyMatch(lower::contains);
    }
}
