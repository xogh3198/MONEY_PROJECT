package com.dividendbot.news.service.video;

import com.dividendbot.news.dto.AiSceneGenerationRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiSceneGenerationServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void storesCompletedGeneratedVideoAsRenderableAsset() {
        AiSceneProvider provider = new AiSceneProvider() {
            @Override
            public boolean configured() {
                return true;
            }

            @Override
            public String name() {
                return "TEST_AI";
            }

            @Override
            public GeneratedSceneVideo generate(AiSceneGenerationRequest request) {
                return new GeneratedSceneVideo(new byte[]{1, 2, 3, 4}, "video/mp4", "scene.mp4", "test");
            }
        };
        VideoAssetStorage storage = new VideoAssetStorage(tempDir.toString());
        AiSceneGenerationService service = new AiSceneGenerationService(provider, storage, Runnable::run);

        var response = service.submit(new AiSceneGenerationRequest(
                "promotion-test",
                2,
                "주문을 한 번에",
                "스마트폰 주문 화면",
                List.of("mobile order"),
                "자연스러운 상업 영상"
        ));

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.assetRef()).endsWith(".mp4");
        assertThat(storage.resolve(response.assetRef()).mediaKind()).isEqualTo(SceneMediaKind.VIDEO);
        assertThat(service.getFile(response.id()).exists()).isTrue();

        service.submit(new AiSceneGenerationRequest(
                "promotion-test",
                3,
                "간편 주문",
                "상품 선택 장면",
                List.of("product order"),
                "자연스러운 상업 영상"
        ));
        assertThatThrownBy(() -> service.submit(new AiSceneGenerationRequest(
                "promotion-test",
                4,
                "다음 행동",
                "CTA 장면",
                List.of("call to action"),
                "자연스러운 상업 영상"
        ))).hasMessageContaining("최대 2개");
    }
}
