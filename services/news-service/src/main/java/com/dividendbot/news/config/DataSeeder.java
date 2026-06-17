package com.dividendbot.news.config;

import com.dividendbot.news.domain.entity.NewsArticle;
import com.dividendbot.news.domain.entity.NewsCategory;
import com.dividendbot.news.domain.entity.NewsSentiment;
import com.dividendbot.news.domain.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 앱 시작 시 뉴스 데이터가 없으면 시드 데이터 자동 생성.
 * 네이버 API 키가 없어도 서비스가 동작하게 보장.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final NewsArticleRepository newsRepository;

    @Override
    public void run(String... args) {
        if (newsRepository.count() > 0) {
            log.info("뉴스 데이터 존재 — 시드 스킵");
            return;
        }

        log.info("뉴스 데이터 없음 — 시드 데이터 생성 중...");

        List<NewsArticle> seeds = List.of(
            article("한은, 기준금리 3.0% 동결...\"하반기 인하 검토\"",
                "한국은행 금융통화위원회가 기준금리를 3.0%로 동결. 이창용 총재는 하반기 경기 둔화 시 인하 가능성을 시사.",
                "한국경제", NewsCategory.RATE, NewsSentiment.NEUTRAL, 2341, 56),
            article("삼성전자, AI 반도체 수주 급증...목표가 상향",
                "HBM3E 공급 확대로 2분기 영업이익 시장 예상 40% 상회 전망. 주요 증권사 목표가 잇따라 상향.",
                "매일경제", NewsCategory.DOMESTIC, NewsSentiment.POSITIVE, 4521, 124),
            article("원/달러 1,350원 돌파...수출기업 수혜 vs 수입물가 부담",
                "미국 고용 호조로 달러 강세 지속. 수출기업에는 호재이나 수입 원자재 부담 가중.",
                "연합뉴스", NewsCategory.FOREX, NewsSentiment.NEGATIVE, 1876, 38),
            article("비트코인 10만 달러 재도전...ETF 자금 유입 지속",
                "기관투자자의 비트코인 ETF 매수세가 지속되며 10만 달러 저항선을 재차 시험 중.",
                "코인데스크", NewsCategory.CRYPTO, NewsSentiment.POSITIVE, 3210, 92),
            article("나스닥 사상 최고치 경신...엔비디아 10% 급등",
                "AI 관련 대형주 실적 기대감 반영. 엔비디아 시간외 10% 급등하며 나스닥 견인.",
                "서울경제", NewsCategory.OVERSEAS, NewsSentiment.POSITIVE, 2890, 67),
            article("한국 수출 5개월 연속 증가, 반도체 36% 성장",
                "반도체 수출이 전년 대비 36% 증가하며 한국 수출을 견인. 5개월 연속 플러스 기록.",
                "한국경제", NewsCategory.DOMESTIC, NewsSentiment.POSITIVE, 1543, 29),
            article("美 연준 위원 \"9월 인하 시기상조\" 발언...증시 혼조",
                "연준 위원의 매파적 발언에 9월 금리인하 기대감 후퇴. 글로벌 증시 혼조세.",
                "연합뉴스", NewsCategory.OVERSEAS, NewsSentiment.NEGATIVE, 2198, 44),
            article("국내 2차전지주 급락...중국 과잉공급 우려",
                "중국 배터리 업체들의 공격적 가격 인하로 국내 2차전지주가 일제히 하락.",
                "매일경제", NewsCategory.DOMESTIC, NewsSentiment.NEGATIVE, 3102, 78)
        );

        newsRepository.saveAll(seeds);
        log.info("시드 데이터 {}건 생성 완료", seeds.size());
    }

    private NewsArticle article(String title, String summary, String source,
                                NewsCategory category, NewsSentiment sentiment,
                                int views, int comments) {
        return NewsArticle.builder()
                .title(title)
                .summary(summary)
                .sourceName(source)
                .sourceUrl("https://news.example.com/" + title.hashCode())
                .category(category)
                .sentiment(sentiment)
                .viewCount(views)
                .commentCount(comments)
                .positiveVotes((int)(Math.random() * 100))
                .negativeVotes((int)(Math.random() * 50))
                .publishedAt(LocalDateTime.now().minusHours((int)(Math.random() * 12)))
                .build();
    }
}
