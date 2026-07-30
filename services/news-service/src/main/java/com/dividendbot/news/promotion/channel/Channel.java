package com.dividendbot.news.promotion.channel;

import java.util.Set;

public record Channel(
        String id,
        String name,
        Set<String> goals,
        String funnelStage,
        String contentType,
        String actionVerb,
        int baseScore,
        long estimatedCostMin,
        long estimatedCostMax,
        int estimatedHours,
        String confidence,
        String sourceUrl,
        String verifiedAt,
        boolean organic
) {
}

