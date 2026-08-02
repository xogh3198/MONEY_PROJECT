package com.dividendbot.news.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReferenceVideoAnalysisRequest(
        @NotBlank @Size(max = 500) String referenceVideoUrl,
        @Size(max = 800) String stylePrompt
) {
}
