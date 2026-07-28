package com.dividendbot.news.service.video;

import java.nio.file.Path;

public record VideoRenderResult(
        Path outputFile,
        double durationSeconds,
        String voiceProvider,
        String assetCredits
) {
}
