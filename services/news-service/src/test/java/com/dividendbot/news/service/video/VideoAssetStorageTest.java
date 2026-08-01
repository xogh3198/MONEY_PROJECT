package com.dividendbot.news.service.video;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VideoAssetStorageTest {

    @TempDir
    Path tempDirectory;

    @Test
    void storesOwnedVideoWithGeneratedSafeReference() {
        VideoAssetStorage storage = new VideoAssetStorage(tempDirectory.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "my clip.mp4",
                "video/mp4",
                new byte[]{0, 1, 2, 3}
        );

        VideoAssetStorage.StoredVideoAsset stored = storage.store(file);

        assertThat(stored.reference()).matches("[0-9a-f-]{36}\\.mp4");
        assertThat(stored.mediaKind()).isEqualTo(SceneMediaKind.VIDEO);
        assertThat(Files.isRegularFile(stored.path())).isTrue();
        assertThat(storage.resolve(stored.reference()).path()).isEqualTo(stored.path());
    }

    @Test
    void rejectsUnsupportedFiles() {
        VideoAssetStorage storage = new VideoAssetStorage(tempDirectory.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "script.svg",
                "image/svg+xml",
                "<svg/>".getBytes()
        );

        assertThatThrownBy(() -> storage.store(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JPG");
    }
}
