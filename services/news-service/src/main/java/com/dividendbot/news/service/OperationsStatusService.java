package com.dividendbot.news.service;

import com.dividendbot.news.domain.entity.ExternalMetricStatus;
import com.dividendbot.news.domain.repository.NewsArticleRepository;
import com.dividendbot.news.dto.OperationsStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OperationsStatusService {

    private final NewsArticleRepository newsRepository;
    private final CollectorRunStateService collectorRunStateService;

    @Value("${naver.datalab.enabled:true}")
    private boolean naverDataLabEnabled;

    @Value("${naver.api.client-id:}")
    private String naverClientId;

    @Value("${naver.api.client-secret:}")
    private String naverClientSecret;

    @Value("${external.metrics.enabled:true}")
    private boolean externalMetricsEnabled;

    @Value("${external.youtube.api-key:}")
    private String youtubeApiKey;

    public OperationsStatusResponse getStatus() {
        Map<ExternalMetricStatus, Long> statusCounts = new EnumMap<>(ExternalMetricStatus.class);
        for (ExternalMetricStatus status : ExternalMetricStatus.values()) {
            statusCounts.put(status, newsRepository.countByExternalMetricStatus(status));
        }

        OperationsStatusResponse.IntegrationConfig integrations =
                new OperationsStatusResponse.IntegrationConfig(
                        naverDataLabEnabled,
                        hasText(naverClientId) && hasText(naverClientSecret),
                        externalMetricsEnabled,
                        hasText(youtubeApiKey)
                );

        OperationsStatusResponse.ArticleMetrics articleMetrics =
                new OperationsStatusResponse.ArticleMetrics(
                        newsRepository.count(),
                        newsRepository.countByViewCountGreaterThan(0),
                        newsRepository.countByCommentCountGreaterThan(0),
                        newsRepository.countArticlesWithInternalVotes(),
                        newsRepository.countArticlesWithExternalValues(),
                        newsRepository.countByExternalSearchInterestIsNotNull(),
                        newsRepository.findLatestExternalMetricsUpdatedAt(),
                        newsRepository.findLatestSearchInterestUpdatedAt(),
                        statusCounts
                );

        return new OperationsStatusResponse(
                LocalDateTime.now(),
                integrations,
                articleMetrics,
                collectorRunStateService.findAll().stream()
                        .map(OperationsStatusResponse.Collector::from)
                        .toList()
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
