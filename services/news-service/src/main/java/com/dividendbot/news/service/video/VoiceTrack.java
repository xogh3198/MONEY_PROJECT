package com.dividendbot.news.service.video;

import java.nio.file.Path;
import java.util.List;

public record VoiceTrack(
        Path audioFile,
        double durationSeconds,
        List<TimedCaption> captions,
        String provider
) {
}
