package com.dividendbot.news.service.video;

import com.dividendbot.news.domain.entity.VideoRenderJob;
import com.dividendbot.news.domain.entity.VideoRenderStatus;
import com.dividendbot.news.domain.repository.VideoRenderJobRepository;
import com.dividendbot.news.dto.VideoRenderJobResponse;
import com.dividendbot.news.dto.VideoRenderRequest;
import com.dividendbot.news.dto.ShortformClipRequest;
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
    private final ShortformSourceStorage shortformSources;

    public VideoRenderService(
            VideoRenderJobRepository repository,
            VideoRenderWorker worker,
            VoiceProviderRouter voiceProviderRouter,
            SceneAssetRenderer sceneAssetRenderer,
            FfmpegVideoRenderer renderer,
            ShortformSourceStorage shortformSources
    ) {
        this.repository = repository;
        this.worker = worker;
        this.voiceProviderRouter = voiceProviderRouter;
        this.sceneAssetRenderer = sceneAssetRenderer;
        this.renderer = renderer;
        this.shortformSources = shortformSources;
    }

    public VideoRenderJobResponse submitClip(ShortformClipRequest request) {
        if (request == null || request.sourceId() == null || !request.rightsConfirmed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "원본 영상과 이용 권한 확인이 필요합니다.");
        }
        ShortformSourceStorage.Source source;
        try { source = shortformSources.resolve(request.sourceId()); }
        catch (IllegalArgumentException error) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error.getMessage(), error); }
        double length = request.endSeconds() - request.startSeconds();
        if (!Double.isFinite(length) || !Double.isFinite(request.startSeconds()) || request.startSeconds() < 0
                || length < 3 || length > 90 || request.endSeconds() > source.durationSeconds() + 0.1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "클립 구간은 원본 범위 내 3~90초여야 합니다.");
        }
        if (request.title() == null || request.title().isBlank() || request.title().length() > 240) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "영상 제목은 1~240자로 입력해주세요.");
        }
        if (request.captions() == null || request.captions().size() > 20) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자막은 최대 20개까지 입력할 수 있습니다.");
        }
        for (ShortformClipRequest.Caption caption : request.captions()) {
            if (caption.text() == null || caption.text().isBlank() || caption.text().length() > 120
                    || !Double.isFinite(caption.startSeconds()) || !Double.isFinite(caption.endSeconds())
                    || caption.startSeconds() < 0 || caption.endSeconds() <= caption.startSeconds()
                    || caption.endSeconds() > length + 0.1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자막 시간·내용이 올바르지 않습니다.");
            }
        }
        UUID jobId = UUID.randomUUID();
        VideoRenderJob job = VideoRenderJob.queued(jobId, "shortform-" + request.sourceId(), request.title(),
                request.quality() == null ? com.dividendbot.news.domain.entity.VideoRenderQuality.PREVIEW : request.quality(), "ORIGINAL_AUDIO");
        repository.save(job);
        try { worker.renderClip(jobId, request); }
        catch (RuntimeException error) {
            job.markFailed("영상 렌더 대기열이 가득 찼습니다.");
            repository.save(job);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "영상 렌더 대기열이 가득 찼습니다.", error);
        }
        return VideoRenderJobResponse.from(job);
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
