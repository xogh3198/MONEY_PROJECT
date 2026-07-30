package com.dividendbot.news.promotion.analysis;

import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class WebsiteAnalysisModels {

    private WebsiteAnalysisModels() {
    }

    public record CreateRequest(
            String sourceType,
            @Size(max = 2048, message = "URL은 2,048자 이하여야 합니다.")
            String url,
            @Size(max = 120, message = "이름은 120자 이하여야 합니다.")
            String title,
            @Size(max = 4000, message = "소개 내용은 4,000자 이하여야 합니다.")
            String description,
            @Size(max = 5, message = "참고 링크는 최대 5개까지 입력할 수 있습니다.")
            List<@Size(max = 2048, message = "참고 링크는 2,048자 이하여야 합니다.") String> referenceLinks
    ) {
    }

    public record Evidence(
            String label,
            String value,
            String sourceUrl
    ) {
    }

    public record Response(
            String analysisId,
            String sourceType,
            String canonicalUrl,
            String status,
            String title,
            String sourceSummary,
            String industry,
            List<String> targetAudienceHypotheses,
            List<String> primaryCtas,
            List<String> serviceRegions,
            List<Evidence> evidence,
            List<String> warnings,
            Instant analyzedAt
    ) {
    }
}
