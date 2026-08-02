package com.dividendbot.news.dto;

public record ReferenceVideoAnalysisResponse(
        String provider,
        String status,
        String sourceUrl,
        String title,
        String author,
        Integer durationSeconds,
        Integer cueCount,
        Double averageCueSeconds,
        Integer openingCueCount,
        Double speechDensity,
        String styleSummary,
        String note
) {
}
