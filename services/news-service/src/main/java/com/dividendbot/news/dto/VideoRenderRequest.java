package com.dividendbot.news.dto;

import com.dividendbot.news.domain.entity.VideoRenderQuality;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record VideoRenderRequest(
        @NotBlank @Size(max = 160) String experimentId,
        @NotBlank @Size(max = 240) String title,
        @NotEmpty @Size(min = 5, max = 8) List<@Valid Scene> scenes,
        @Size(max = 500) String disclaimer,
        @Size(max = 500) String aiDisclosure,
        VideoRenderQuality quality
) {
    public VideoRenderQuality normalizedQuality() {
        return quality == null ? VideoRenderQuality.PREVIEW : quality;
    }

    public record Scene(
            int order,
            @NotBlank @Size(max = 800) String narration,
            @NotBlank @Size(max = 100) String onScreenText,
            @Size(max = 240) String visualDirection,
            @Size(max = 5) List<@Size(max = 80) String> visualSearchTerms
    ) {
    }
}
