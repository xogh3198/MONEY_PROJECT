package com.dividendbot.news.service.video;

import java.nio.file.Path;

public interface VoiceProvider {
    String name();

    boolean configured();

    VoiceTrack synthesize(String narration, Path outputDirectory, String fileStem);
}
