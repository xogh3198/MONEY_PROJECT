package com.dividendbot.news.dto;

import com.dividendbot.news.domain.entity.VideoRenderQuality;

import java.util.List;
import java.util.UUID;

public record ShortformClipRequest(
        UUID sourceId,
        String title,
        double startSeconds,
        double endSeconds,
        VideoRenderQuality quality,
        List<Caption> captions,
        boolean rightsConfirmed
) {
    public record Caption(double startSeconds, double endSeconds, String text) {}
}
