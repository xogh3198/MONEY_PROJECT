package com.dividendbot.news.service.video;

import com.dividendbot.news.domain.entity.VideoRenderQuality;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FfmpegVideoRendererTest {

    @TempDir
    Path tempDirectory;

    @Test
    void rendersUploadedVideoLoopWithGeneratedVoiceAsTheOnlyAudioTrack() {
        ExternalProcessRunner runner = mock(ExternalProcessRunner.class);
        FfmpegVideoRenderer renderer = new FfmpegVideoRenderer(
                mock(VoiceProviderRouter.class),
                mock(SceneAssetRenderer.class),
                mock(AssSubtitleWriter.class),
                runner,
                mock(MediaProbe.class),
                tempDirectory.toString(),
                "ffmpeg"
        );
        Path video = tempDirectory.resolve("owned.mp4");
        Path audio = tempDirectory.resolve("voice.wav");
        Path subtitles = tempDirectory.resolve("captions.ass");
        Path output = tempDirectory.resolve("scene.mp4");

        renderer.renderScene(
                new RenderedSceneAsset(video, "사용자 제공", "USER_UPLOAD", SceneMediaKind.VIDEO),
                new VoiceTrack(audio, 5.25, List.of(), "TYPECAST"),
                subtitles,
                output,
                VideoRenderQuality.PREVIEW
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> command = ArgumentCaptor.forClass(List.class);
        verify(runner).run(command.capture(), any(Duration.class));
        assertThat(command.getValue()).containsSubsequence("-stream_loop", "-1", "-i", video.toString());
        assertThat(command.getValue()).containsSubsequence("-map", "0:v:0", "-map", "1:a:0");
        assertThat(command.getValue()).contains(audio.toString(), output.toString());
    }
}
