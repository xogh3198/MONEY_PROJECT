package com.dividendbot.news.service.video;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShortformSourceStorageTest {
    @TempDir Path directory;

    @Test
    void uploadsChunksInOrderAndCompletesOnlyWhenFull() {
        MediaProbe probe = mock(MediaProbe.class);
        ShortformSourceStorage storage = new ShortformSourceStorage(directory.toString(), probe);
        UUID id = storage.start(6, "video/mp4");
        assertThat(storage.append(id, 0, new byte[]{1, 2, 3})).isEqualTo(3);
        assertThatThrownBy(() -> storage.append(id, 0, new byte[]{4})).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.complete(id)).isInstanceOf(IllegalArgumentException.class);
        assertThat(storage.append(id, 3, new byte[]{4, 5, 6})).isEqualTo(6);
        when(probe.durationSeconds(directory.resolve("shortform-sources").resolve(id.toString()).resolve("source.mp4"))).thenReturn(38.0);
        assertThat(storage.complete(id).durationSeconds()).isEqualTo(38.0);
        assertThat(storage.resolve(id).id()).isEqualTo(id);
        assertThatThrownBy(() -> storage.append(id, 6, new byte[]{7})).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsOversizedOrUnsupportedUploads() {
        ShortformSourceStorage storage = new ShortformSourceStorage(directory.toString(), mock(MediaProbe.class));
        assertThatThrownBy(() -> storage.start(ShortformSourceStorage.MAX_SOURCE_BYTES + 1, "video/mp4"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.start(1, "image/png")).isInstanceOf(IllegalArgumentException.class);
    }
}
