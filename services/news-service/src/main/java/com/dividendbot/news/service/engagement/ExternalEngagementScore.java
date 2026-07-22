package com.dividendbot.news.service.engagement;

public final class ExternalEngagementScore {
    private ExternalEngagementScore() {}

    public static int calculate(ExternalEngagementMetrics metrics) {
        if (metrics == null || !metrics.hasAnyCount()) return 0;

        double score = weightedLog(metrics.views(), 4.0)
                + weightedLog(metrics.comments(), 12.0)
                + weightedLog(metrics.positive(), 8.0)
                + weightedLog(metrics.negative(), 4.0);
        return Math.max(0, Math.min(200, (int) Math.round(score)));
    }

    private static double weightedLog(Long value, double weight) {
        if (value == null || value <= 0) return 0;
        return Math.log1p(value) * weight;
    }
}
