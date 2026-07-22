package com.dividendbot.news.service.engagement;

import com.dividendbot.news.domain.entity.ExternalMetricStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalEngagementScoreTest {

    @Test
    void capsVeryLargeExternalCounts() {
        ExternalEngagementMetrics metrics = new ExternalEngagementMetrics(
                100_000_000L, 1_000_000L, 2_000_000L, 100_000L, "TEST", ExternalMetricStatus.AVAILABLE);
        assertThat(ExternalEngagementScore.calculate(metrics)).isBetween(1, 200);
    }

    @Test
    void emptyMetricsHaveZeroScore() {
        assertThat(ExternalEngagementScore.calculate(
                ExternalEngagementMetrics.unavailable(ExternalMetricStatus.NOT_SUPPORTED, "TEST")
        )).isZero();
    }
}
