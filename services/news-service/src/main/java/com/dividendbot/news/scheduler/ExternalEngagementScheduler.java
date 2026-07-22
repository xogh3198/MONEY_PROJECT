package com.dividendbot.news.scheduler;

import com.dividendbot.news.domain.entity.NewsArticle;
import com.dividendbot.news.domain.repository.NewsArticleRepository;
import com.dividendbot.news.service.engagement.ExternalEngagementCollector;
import com.dividendbot.news.service.engagement.ExternalEngagementMetrics;
import com.dividendbot.news.service.engagement.ExternalEngagementScore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@ConditionalOnProperty(name = "external.metrics.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class ExternalEngagementScheduler {
    private final NewsArticleRepository newsRepository;
    private final ExternalEngagementCollector collector;

    @Value("${external.metrics.refresh-hours:6}")
    private int refreshHours;

    @Value("${external.metrics.batch-size:12}")
    private int batchSize;

    @Scheduled(
            fixedDelayString = "${external.metrics.interval-ms:3600000}",
            initialDelayString = "${external.metrics.initial-delay-ms:120000}"
    )
    public void refreshExternalMetrics() {
        LocalDateTime now = LocalDateTime.now();
        List<NewsArticle> candidates = newsRepository.findExternalMetricRefreshCandidates(
                now.minusDays(7),
                now.minusHours(Math.max(1, refreshHours)),
                PageRequest.of(0, Math.max(1, Math.min(batchSize, 50)))
        );
        if (candidates.isEmpty()) return;

        int available = 0;
        for (NewsArticle article : candidates) {
            ExternalEngagementMetrics metrics = collector.collect(article.getSourceUrl());
            article.updateExternalMetrics(
                    metrics.views(),
                    metrics.comments(),
                    metrics.positive(),
                    metrics.negative(),
                    ExternalEngagementScore.calculate(metrics),
                    metrics.provider(),
                    metrics.status(),
                    LocalDateTime.now()
            );
            newsRepository.save(article);
            if (metrics.hasAnyCount()) available++;
        }
        log.info("외부 참여 지표 갱신: 처리 {}건, 공개 수치 확인 {}건", candidates.size(), available);
    }
}
