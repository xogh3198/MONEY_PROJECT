package com.dividendbot.news.service.video;

import java.nio.file.Path;

public record RenderedSceneAsset(
        Path imageFile,
        String credit,
        String source
) {
}
