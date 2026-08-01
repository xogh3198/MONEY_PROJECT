package com.dividendbot.news.service.video;

import java.nio.file.Path;

public record RenderedSceneAsset(
        Path mediaFile,
        String credit,
        String source,
        SceneMediaKind mediaKind
) {
}
