package com.dividendbot.news.service.video;

import com.dividendbot.news.domain.entity.VideoRenderQuality;
import com.dividendbot.news.dto.ShortformClipRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FfmpegShortformClipRendererTest {
    @TempDir Path directory;

    @Test
    void rendersOwnedSourceWithOriginalAudioAndTimedCaptions() throws Exception {
        ShortformSourceStorage sources = mock(ShortformSourceStorage.class);
        ExternalProcessRunner runner = mock(ExternalProcessRunner.class);
        MediaProbe probe = mock(MediaProbe.class);
        UUID sourceId = UUID.randomUUID();
        Path source = directory.resolve("owned.mp4");
        Files.write(source, new byte[]{1});
        when(sources.resolve(sourceId)).thenReturn(new ShortformSourceStorage.Source(sourceId, source, 120));
        when(probe.durationSeconds(any(Path.class))).thenReturn(25.0);
        FfmpegShortformClipRenderer renderer = new FfmpegShortformClipRenderer(
                sources, new AssSubtitleWriter(), runner, probe, directory.toString(), "ffmpeg"
        );
        ShortformClipRequest request = new ShortformClipRequest(sourceId, "직접 만든 영상", 10, 35,
                VideoRenderQuality.PREVIEW, List.of(new ShortformClipRequest.Caption(0, 3, "첫 자막")), true);

        VideoRenderResult result = renderer.render(UUID.randomUUID(), request, progress -> {});

        ArgumentCaptor<List<String>> command = ArgumentCaptor.forClass(List.class);
        verify(runner).run(command.capture(), any(Duration.class));
        assertThat(command.getValue()).contains("-ss", "10.000", "-t", "25.000", "0:v:0", "0:a:0?", "-c:a", "aac");
        assertThat(result.voiceProvider()).isEqualTo("ORIGINAL_AUDIO");
        assertThat(result.durationSeconds()).isEqualTo(25.0);
        assertThat(Files.readString(result.outputFile().getParent().resolve("captions.ass"))).contains("첫 자막");
    }
}
