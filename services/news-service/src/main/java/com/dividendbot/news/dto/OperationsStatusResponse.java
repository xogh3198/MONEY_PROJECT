package com.dividendbot.news.dto;

import com.dividendbot.news.domain.entity.CollectorRunState;
import com.dividendbot.news.domain.entity.ExternalMetricStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record OperationsStatusResponse(
        LocalDateTime generatedAt,
        IntegrationConfig integrations,
        ArticleMetrics articles,
        List<Collector> collectors
) {
    public record IntegrationConfig(
            boolean naverDataLabEnabled,
            boolean naverCredentialsConfigured,
            boolean externalMetricsEnabled,
            boolean youtubeApiConfigured
    ) {
    }

    public record ArticleMetrics(
            long total,
            long internalViewed,
            long internalCommented,
            long internalVoted,
            long externalValuesAvailable,
            long searchInterestAvailable,
            LocalDateTime lastExternalMetricsUpdate,
            LocalDateTime lastSearchInterestUpdate,
            Map<ExternalMetricStatus, Long> externalStatusCounts
    ) {
    }

    public record Collector(
            String name,
            String status,
            boolean enabled,
            boolean configured,
            LocalDateTime lastAttemptAt,
            LocalDateTime lastSuccessAt,
            LocalDateTime lastFailureAt,
            Long lastDurationMs,
            Integer processedCount,
            Integer availableCount,
            String message
    ) {
        public static Collector from(CollectorRunState state) {
            return new Collector(
                    state.getCollectorName(),
                    state.getStatus().name(),
                    state.isEnabled(),
                    state.isConfigured(),
                    state.getLastAttemptAt(),
                    state.getLastSuccessAt(),
                    state.getLastFailureAt(),
                    state.getLastDurationMs(),
                    state.getProcessedCount(),
                    state.getAvailableCount(),
                    state.getMessage()
            );
        }
    }
}
