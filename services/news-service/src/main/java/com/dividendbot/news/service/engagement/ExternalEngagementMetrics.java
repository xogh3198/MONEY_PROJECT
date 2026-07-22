package com.dividendbot.news.service.engagement;

import com.dividendbot.news.domain.entity.ExternalMetricStatus;

public record ExternalEngagementMetrics(
        Long views,
        Long comments,
        Long positive,
        Long negative,
        String provider,
        ExternalMetricStatus status
) {
    public static ExternalEngagementMetrics unavailable(ExternalMetricStatus status, String provider) {
        return new ExternalEngagementMetrics(null, null, null, null, provider, status);
    }

    public boolean hasAnyCount() {
        return views != null || comments != null || positive != null || negative != null;
    }
}
