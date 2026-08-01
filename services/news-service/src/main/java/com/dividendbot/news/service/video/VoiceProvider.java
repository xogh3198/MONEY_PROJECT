package com.dividendbot.news.service.video;

import com.dividendbot.news.domain.entity.VideoVoiceStyle;

import java.nio.file.Path;

public interface VoiceProvider {
    String name();

    boolean configured();

    VoiceTrack synthesize(
            String narration,
            Path outputDirectory,
            String fileStem,
            VideoVoiceStyle voiceStyle
    );

    default VoiceTrack synthesize(
            String narration,
            Path outputDirectory,
            String fileStem,
            VideoVoiceStyle voiceStyle,
            String voiceId
    ) {
        return synthesize(narration, outputDirectory, fileStem, voiceStyle);
    }

    default String displayName() {
        return name();
    }

    default String tier() {
        return "FREE";
    }

    default java.util.List<java.util.Map<String, Object>> availableVoices() {
        return java.util.List.of();
    }
}
