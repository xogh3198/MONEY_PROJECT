package com.dividendbot.news.scheduler;

import com.dividendbot.news.domain.entity.NewsArticle;
import com.dividendbot.news.domain.repository.NewsArticleRepository;
import com.dividendbot.news.service.engagement.ExternalEngagementCollector;
import com.dividendbot.news.service.engagement.ExternalEngagementMetrics;
import com.dividendbot.news.service.engagement.ExternalEngagementScore;
import com.dividendbot.news.service.CollectorRunStateService;
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
    private final CollectorRunStateService runStateService;

    @Value("${external.metrics.refresh-hours:6}")
    private int refreshHours;

    @Value("${external.metrics.batch-size:12}")
    private int batchSize;

    @Scheduled(
            fixedDelayString = "${external.metrics.interval-ms:900000}",
            initialDelayString = "${external.metrics.initial-delay-ms:120000}"
    )
    public void refreshExternalMetrics() {
        long startedAt = System.nanoTime();
        recordSafely(() -> runStateService.markRunning(
                CollectorRunStateService.EXTERNAL_ENGAGEMENT,
                true,
                true
        ));

        try {
            LocalDateTime now = LocalDateTime.now();
            List<NewsArticle> candidates = newsRepository.findExternalMetricRefreshCandidates(
                    now.minusDays(7),
                    now.minusHours(Math.max(1, refreshHours)),
                    PageRequest.of(0, Math.max(1, Math.min(batchSize, 50)))
            );
            if (candidates.isEmpty()) {
                recordSafely(() -> runStateService.markSuccess(
                        CollectorRunStateService.EXTERNAL_ENGAGEMENT,
                        0,
                        0,
                        elapsedMillis(startedAt),
                        "갱신 대상 기사가 없습니다."
                ));
                return;
            }

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
            int finalAvailable = available;
            recordSafely(() -> runStateService.markSuccess(
                    CollectorRunStateService.EXTERNAL_ENGAGEMENT,
                    candidates.size(),
                    finalAvailable,
                    elapsedMillis(startedAt),
                    "후보 " + candidates.size() + "건 중 공개 수치 " + finalAvailable + "건을 확인했습니다."
            ));
            log.info("외부 참여 지표 갱신: 처리 {}건, 공개 수치 확인 {}건", candidates.size(), available);
        } catch (Exception e) {
            recordSafely(() -> runStateService.markFailed(
                    CollectorRunStateService.EXTERNAL_ENGAGEMENT,
                    elapsedMillis(startedAt),
                    e.getClass().getSimpleName() + ": " + safeMessage(e)
            ));
            log.warn("외부 참여 지표 갱신 실패: {}", e.getMessage());
        }
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
            log.warn("외부 참여 실행 상태 저장 실패: {}", statusError.getMessage());
        }
    }
}
