package com.dividendbot.news.service.video;

import com.dividendbot.news.domain.entity.VideoRenderJob;
import com.dividendbot.news.domain.entity.VideoRenderStatus;
import com.dividendbot.news.domain.repository.VideoRenderJobRepository;
import com.dividendbot.news.dto.VideoRenderJobResponse;
import com.dividendbot.news.dto.VideoRenderRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import com.dividendbot.news.domain.entity.VideoVoiceStyle;

@Service
public class VideoRenderService {

    private final VideoRenderJobRepository repository;
    private final VideoRenderWorker worker;
    private final VoiceProviderRouter voiceProviderRouter;
    private final SceneAssetRenderer sceneAssetRenderer;
    private final FfmpegVideoRenderer renderer;

    public VideoRenderService(
            VideoRenderJobRepository repository,
            VideoRenderWorker worker,
            VoiceProviderRouter voiceProviderRouter,
            SceneAssetRenderer sceneAssetRenderer,
            FfmpegVideoRenderer renderer
    ) {
        this.repository = repository;
        this.worker = worker;
        this.voiceProviderRouter = voiceProviderRouter;
        this.sceneAssetRenderer = sceneAssetRenderer;
        this.renderer = renderer;
    }

    public VideoRenderJobResponse submit(VideoRenderRequest request) {
        if (!voiceProviderRouter.selectedConfigured()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    voiceProviderRouter.selectedName() + " 음성 공급자 설정이 필요합니다."
            );
        }
        UUID jobId = UUID.randomUUID();
        VideoRenderJob job = VideoRenderJob.queued(
                jobId,
                request.experimentId(),
                request.title(),
                request.normalizedQuality(),
                voiceProviderRouter.selectedName()
        );
        repository.save(job);
        try {
            worker.render(jobId, request);
        } catch (RuntimeException error) {
            job.markFailed("영상 렌더 대기열이 가득 찼습니다. 잠시 후 다시 시도해주세요.");
            repository.save(job);
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "영상 렌더 대기열이 가득 찼습니다.",
                    error
            );
        }
        return VideoRenderJobResponse.from(job);
    }

    public VideoRenderJobResponse get(UUID jobId) {
        return VideoRenderJobResponse.from(requireJob(jobId));
    }

    public Resource getFile(UUID jobId) {
        VideoRenderJob job = requireJob(jobId);
        if (job.getStatus() != VideoRenderStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "아직 영상 렌더링이 완료되지 않았습니다.");
        }
        Path file = renderer.resolveOutput(jobId, job.getOutputFileName());
        if (!Files.isRegularFile(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "완료된 영상 파일을 찾을 수 없습니다.");
        }
        return new FileSystemResource(file);
    }

    public String fileName(UUID jobId) {
        return requireJob(jobId).getOutputFileName();
    }

    public Map<String, Object> capabilities() {
        return Map.ofEntries(
                Map.entry("selectedVoiceProvider", voiceProviderRouter.selectedName()),
                Map.entry("voiceConfigured", voiceProviderRouter.selectedConfigured()),
                Map.entry("availableVoiceProviders", voiceProviderRouter.availableProviders()),
                Map.entry("supportedVoiceStyles", voiceProviderRouter.supportedStyles()),
                Map.entry("voiceCatalog", voiceProviderRouter.voiceCatalog()),
                Map.entry("pixabayConfigured", sceneAssetRenderer.pixabayConfigured()),
                Map.entry("ownedMediaUpload", true),
                Map.entry("fallbackAssets", true),
                Map.entry("formats", Map.of(
                        "preview", "540x960 MP4",
                        "final", "1080x1920 MP4"
                ))
        );
    }

    public VoiceTrack previewVoice(String provider, String voiceId, String text, String style, Path outputDir) {
        VideoVoiceStyle voiceStyle;
        try {
            voiceStyle = VideoVoiceStyle.valueOf(style.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            voiceStyle = VideoVoiceStyle.NATURAL;
        }
        String previewText = text.length() > 100 ? text.substring(0, 100) : text;
        return voiceProviderRouter.synthesize(previewText, outputDir, "preview", voiceStyle, provider, voiceId);
    }

    private VideoRenderJob requireJob(UUID jobId) {
        return repository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "영상 작업을 찾을 수 없습니다."
                ));
    }
}
