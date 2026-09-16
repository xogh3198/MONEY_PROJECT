package com.dividendbot.news.service.video;

import com.dividendbot.news.domain.entity.VideoRenderQuality;
import com.dividendbot.news.dto.ShortformClipRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

@Component
public class FfmpegShortformClipRenderer {
    private final ShortformSourceStorage sources;
    private final AssSubtitleWriter subtitleWriter;
    private final ExternalProcessRunner runner;
    private final MediaProbe probe;
    private final Path outputRoot;
    private final String ffmpegPath;

    public FfmpegShortformClipRenderer(
            ShortformSourceStorage sources, AssSubtitleWriter subtitleWriter, ExternalProcessRunner runner, MediaProbe probe,
            @Value("${video.render.storage-path:/var/lib/investboard/videos}") String storagePath,
            @Value("${video.render.ffmpeg-path:ffmpeg}") String ffmpegPath
    ) {
        this.sources = sources;
        this.subtitleWriter = subtitleWriter;
        this.runner = runner;
        this.probe = probe;
        this.outputRoot = Path.of(storagePath).toAbsolutePath().normalize();
        this.ffmpegPath = ffmpegPath;
    }

    public VideoRenderResult render(UUID jobId, ShortformClipRequest request, Consumer<VideoRenderProgress> progress) {
        try {
            ShortformSourceStorage.Source source = sources.resolve(request.sourceId());
            VideoRenderQuality quality = request.quality() == null ? VideoRenderQuality.PREVIEW : request.quality();
            Path directory = outputRoot.resolve(jobId.toString());
            Files.createDirectories(directory);
            double duration = request.endSeconds() - request.startSeconds();
            List<TimedCaption> captions = request.captions() == null ? List.of() : request.captions().stream()
                    .map(c -> new TimedCaption(c.startSeconds(), c.endSeconds(), c.text())).toList();
            Path subtitles = subtitleWriter.write(captions, quality.width(), quality.height(), directory.resolve("captions.ass"));
            String filter = "scale=" + quality.width() + ":" + quality.height()
                    + ":force_original_aspect_ratio=increase,crop=" + quality.width() + ":" + quality.height()
                    + ",setsar=1,fps=" + quality.fps()
                    + ",ass=filename='" + subtitles.toAbsolutePath().toString().replace("\\", "\\\\").replace(":", "\\:").replace("'", "\\'") + "'";
            Path output = directory.resolve("shortform-" + quality.name().toLowerCase(Locale.ROOT) + ".mp4");
            List<String> command = new ArrayList<>(List.of(ffmpegPath, "-hide_banner", "-loglevel", "error", "-y",
                    "-ss", String.format(Locale.ROOT, "%.3f", request.startSeconds()),
                    "-i", source.path().toString(), "-t", String.format(Locale.ROOT, "%.3f", duration),
                    "-map", "0:v:0", "-map", "0:a:0?", "-vf", filter,
                    "-c:v", "libx264", "-preset", "veryfast", "-crf", Integer.toString(quality.crf()),
                    "-pix_fmt", "yuv420p", "-threads", "1", "-c:a", "aac", "-b:a", "128k",
                    "-movflags", "+faststart", output.toString()));
            progress.accept(new VideoRenderProgress("세로 화면·자막·원본 음성 렌더링", 20));
            runner.run(command, Duration.ofMinutes(12));
            progress.accept(new VideoRenderProgress("출력 파일 검증", 95));
            return new VideoRenderResult(output, probe.durationSeconds(output), "ORIGINAL_AUDIO", "사용자가 권리를 확인한 원본 영상");
        } catch (RuntimeException | java.io.IOException error) {
            throw new IllegalStateException("쇼츠 편집에 실패했습니다: " + error.getMessage(), error);
        }
    }
}
