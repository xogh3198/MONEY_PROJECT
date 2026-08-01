package com.dividendbot.news.service.video;

import com.dividendbot.news.dto.VideoRenderRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SceneAssetRendererTest {

    @TempDir
    Path tempDirectory;

    @Test
    void createsRightsSafeFallbackCardWhenPixabayIsNotConfigured() throws Exception {
        VideoAssetStorage assetStorage = new VideoAssetStorage(tempDirectory.toString());
        SceneAssetRenderer renderer = new SceneAssetRenderer(new ObjectMapper(), "", assetStorage);
        VideoRenderRequest.Scene scene = new VideoRenderRequest.Scene(
                1,
                "오늘 시장에서 확인할 핵심 내용입니다.",
                "환율이 움직인 진짜 이유",
                "차트와 환율 이미지",
                List.of("환율", "금융시장"),
                null
        );
        Path output = tempDirectory.resolve("scene.png");

        RenderedSceneAsset result = renderer.render(scene, 540, 960, output);

        assertThat(result.source()).isEqualTo("GENERATED");
        assertThat(Files.isRegularFile(output)).isTrue();
        BufferedImage image = ImageIO.read(output.toFile());
        assertThat(image.getWidth()).isEqualTo(540);
        assertThat(image.getHeight()).isEqualTo(960);
    }
}
