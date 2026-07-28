package com.dividendbot.news.service.video;

import com.dividendbot.news.domain.entity.VideoRenderQuality;
import com.dividendbot.news.dto.VideoRenderRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
public class FfmpegVideoRenderer {

    private final VoiceProviderRouter voiceProviderRouter;
    private final SceneAssetRenderer sceneAssetRenderer;
    private final AssSubtitleWriter subtitleWriter;
    private final ExternalProcessRunner processRunner;
    private final MediaProbe mediaProbe;
    private final Path storageRoot;
    private final String ffmpegPath;

    public FfmpegVideoRenderer(
            VoiceProviderRouter voiceProviderRouter,
            SceneAssetRenderer sceneAssetRenderer,
            AssSubtitleWriter subtitleWriter,
            ExternalProcessRunner processRunner,
            MediaProbe mediaProbe,
            @Value("${video.render.storage-path:/var/lib/investboard/videos}") String storagePath,
            @Value("${video.render.ffmpeg-path:ffmpeg}") String ffmpegPath
    ) {
        this.voiceProviderRouter = voiceProviderRouter;
        this.sceneAssetRenderer = sceneAssetRenderer;
        this.subtitleWriter = subtitleWriter;
        this.processRunner = processRunner;
        this.mediaProbe = mediaProbe;
        this.storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
        this.ffmpegPath = ffmpegPath;
    }

    public VideoRenderResult render(
            UUID jobId,
            VideoRenderRequest request,
            Consumer<VideoRenderProgress> progress
    ) {
        try {
            VideoRenderQuality quality = request.normalizedQuality();
            Path jobDirectory = resolveJobDirectory(jobId);
            Files.createDirectories(jobDirectory);
            List<Path> sceneFiles = new ArrayList<>();
            List<String> credits = new ArrayList<>();
            String voiceProvider = voiceProviderRouter.selectedName();

            int sceneCount = request.scenes().size();
            for (int index = 0; index < sceneCount; index++) {
                VideoRenderRequest.Scene scene = request.scenes().get(index);
                int number = index + 1;
                int baseProgress = 5 + (int) Math.floor((number - 1) * 78d / sceneCount);
                progress.accept(new VideoRenderProgress(
                        "장면 " + number + "/" + sceneCount + " 음성 생성",
                        baseProgress
                ));

                String stem = String.format("scene-%02d", number);
                VoiceTrack voice = voiceProviderRouter.synthesize(scene.narration(), jobDirectory, stem);
                voiceProvider = voice.provider();

                progress.accept(new VideoRenderProgress(
                        "장면 " + number + "/" + sceneCount + " 이미지·자막 구성",
                        baseProgress + Math.max(1, 30 / sceneCount)
                ));
                Path imageFile = jobDirectory.resolve(stem + ".png");
                RenderedSceneAsset asset = sceneAssetRenderer.render(
                        scene,
                        quality.width(),
                        quality.height(),
                        imageFile
                );
                credits.add(asset.credit());
                Path subtitleFile = subtitleWriter.write(
                        voice.captions(),
                        quality.width(),
                        quality.height(),
                        jobDirectory.resolve(stem + ".ass")
                );

                progress.accept(new VideoRenderProgress(
                        "장면 " + number + "/" + sceneCount + " 영상 렌더링",
                        baseProgress + Math.max(2, 55 / sceneCount)
                ));
                Path sceneFile = jobDirectory.resolve(stem + ".mp4");
                renderScene(imageFile, voice, subtitleFile, sceneFile, quality);
                sceneFiles.add(sceneFile);
            }

            progress.accept(new VideoRenderProgress("장면 연결 및 최적화", 90));
            Path outputFile = jobDirectory.resolve(quality.name().toLowerCase() + ".mp4");
            concatenate(sceneFiles, outputFile, jobDirectory.resolve("concat.txt"));
            double duration = mediaProbe.durationSeconds(outputFile);
            progress.accept(new VideoRenderProgress("파일 검증", 97));
            return new VideoRenderResult(
                    outputFile,
                    duration,
                    voiceProvider,
                    credits.stream().distinct().collect(Collectors.joining("\n"))
            );
        } catch (Exception e) {
            throw new IllegalStateException("세로 영상 렌더링에 실패했습니다: " + safeMessage(e), e);
        }
    }

    public Path resolveOutput(UUID jobId, String outputFileName) {
        if (outputFileName == null || outputFileName.isBlank()) {
            throw new IllegalStateException("완료된 영상 파일 정보가 없습니다.");
        }
        Path jobDirectory = resolveJobDirectory(jobId);
        Path file = jobDirectory.resolve(outputFileName).normalize();
        if (!file.startsWith(jobDirectory)) {
            throw new IllegalStateException("잘못된 영상 파일 경로입니다.");
        }
        return file;
    }

    private Path resolveJobDirectory(UUID jobId) {
        Path directory = storageRoot.resolve(jobId.toString()).normalize();
        if (!directory.startsWith(storageRoot)) {
            throw new IllegalStateException("잘못된 영상 작업 경로입니다.");
        }
        return directory;
    }

    private void renderScene(
            Path imageFile,
            VoiceTrack voice,
            Path subtitleFile,
            Path outputFile,
            VideoRenderQuality quality
    ) {
        String videoFilter = "zoompan=z='min(zoom+0.00045,1.06)'"
                + ":x='iw/2-(iw/zoom/2)'"
                + ":y='ih/2-(ih/zoom/2)'"
                + ":d=" + Math.max(1, (int) Math.ceil(voice.durationSeconds() * quality.fps()))
                + ":s=" + quality.width() + "x" + quality.height()
                + ":fps=" + quality.fps()
                + ",ass=filename='" + escapeFilterPath(subtitleFile.toAbsolutePath()) + "'";

        processRunner.run(
                List.of(
                        ffmpegPath,
                        "-hide_banner", "-loglevel", "error", "-y",
                        "-loop", "1",
                        "-framerate", String.valueOf(quality.fps()),
                        "-i", imageFile.toAbsolutePath().toString(),
                        "-i", voice.audioFile().toAbsolutePath().toString(),
                        "-vf", videoFilter,
                        "-t", String.format(java.util.Locale.ROOT, "%.3f", voice.durationSeconds()),
                        "-c:v", "libx264",
                        "-preset", "veryfast",
                        "-crf", String.valueOf(quality.crf()),
                        "-pix_fmt", "yuv420p",
                        "-threads", "1",
                        "-c:a", "aac",
                        "-b:a", "128k",
                        "-ar", "44100",
                        "-shortest",
                        outputFile.toAbsolutePath().toString()
                ),
                Duration.ofMinutes(5)
        );
    }

    private void concatenate(List<Path> sceneFiles, Path outputFile, Path concatFile) {
        if (sceneFiles.isEmpty()) throw new IllegalStateException("연결할 영상 장면이 없습니다.");
        StringBuilder contents = new StringBuilder();
        for (Path sceneFile : sceneFiles) {
            String escaped = sceneFile.toAbsolutePath().toString().replace("'", "'\\''");
            contents.append("file '").append(escaped).append("'\n");
        }
        try {
            Files.writeString(
                    concatFile,
                    contents,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (Exception e) {
            throw new IllegalStateException("영상 연결 목록을 만들지 못했습니다.", e);
        }

        processRunner.run(
                List.of(
                        ffmpegPath,
                        "-hide_banner", "-loglevel", "error", "-y",
                        "-f", "concat",
                        "-safe", "0",
                        "-i", concatFile.toAbsolutePath().toString(),
                        "-c", "copy",
                        "-movflags", "+faststart",
                        outputFile.toAbsolutePath().toString()
                ),
                Duration.ofMinutes(5)
        );
    }

    private String escapeFilterPath(Path path) {
        return path.toString()
                .replace("\\", "\\\\")
                .replace(":", "\\:")
                .replace("'", "\\'");
    }

    private String safeMessage(Exception error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) return error.getClass().getSimpleName();
        String normalized = message.replaceAll("[\\r\\n\\t]+", " ").trim();
        return normalized.length() <= 700 ? normalized : normalized.substring(0, 700);
    }
}
