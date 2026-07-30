package com.dividendbot.news.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record GrowthEventRequest(
        @NotBlank @Size(max = 64) String eventName,
        @NotBlank @Size(min = 16, max = 80) String visitorId,
        @NotBlank @Size(min = 16, max = 80) String sessionId,
        @NotBlank @Size(max = 255) String path,
        @Size(max = 120) String utmSource,
        @Size(max = 120) String utmMedium,
        @Size(max = 120) String utmCampaign,
        @Size(max = 120) String utmContent,
        Map<String, Object> properties
) {
}
