package com.dividendbot.news.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record GrowthAnalyticsSummary(
        int days,
        long totalEvents,
        long uniqueVisitors,
        long qualifiedVisitors,
        Map<String, Long> events,
        Map<String, Long> campaigns,
        Map<String, Long> paths,
        LocalDateTime generatedAt
) {
}
