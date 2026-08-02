package com.dividendbot.news.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AiSceneGenerationRequest(
        @NotBlank @Size(max = 160) String experimentId,
        @Min(1) @Max(8) int sceneOrder,
        @NotBlank @Size(max = 100) String onScreenText,
        @Size(max = 240) String visualDirection,
        @Size(max = 5) List<@Size(max = 80) String> visualSearchTerms,
        @Size(max = 800) String stylePrompt
) {
}
