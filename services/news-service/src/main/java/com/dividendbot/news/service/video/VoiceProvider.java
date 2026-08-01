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
}
