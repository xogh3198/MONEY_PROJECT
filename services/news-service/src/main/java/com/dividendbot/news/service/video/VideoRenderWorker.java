package com.dividendbot.news.service.video;

import com.dividendbot.news.domain.entity.VideoRenderJob;
import com.dividendbot.news.domain.repository.VideoRenderJobRepository;
import com.dividendbot.news.dto.VideoRenderRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class VideoRenderWorker {

    private final VideoRenderJobRepository repository;
    private final FfmpegVideoRenderer renderer;

    public VideoRenderWorker(VideoRenderJobRepository repository, FfmpegVideoRenderer renderer) {
        this.repository = repository;
        this.renderer = renderer;
    }

    @Async("videoRenderExecutor")
    public void render(UUID jobId, VideoRenderRequest request) {
        update(jobId, "작업 시작", 1);
        try {
            VideoRenderResult result = renderer.render(
                    jobId,
                    request,
                    progress -> update(jobId, progress.stage(), progress.percent())
            );
            VideoRenderJob job = requireJob(jobId);
            job.markCompleted(
                    result.outputFile().getFileName().toString(),
                    result.durationSeconds(),
                    result.assetCredits()
            );
            repository.save(job);
        } catch (Exception e) {
            VideoRenderJob job = requireJob(jobId);
            job.markFailed(safeMessage(e));
            repository.save(job);
        }
    }

    private void update(UUID jobId, String stage, int progress) {
        VideoRenderJob job = requireJob(jobId);
        job.markRendering(stage, progress);
        repository.save(job);
    }

    private VideoRenderJob requireJob(UUID jobId) {
        return repository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("영상 작업을 찾을 수 없습니다."));
    }

    private String safeMessage(Exception error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) return error.getClass().getSimpleName();
        String normalized = message.replaceAll("[\\r\\n\\t]+", " ").trim();
        return normalized.length() <= 900 ? normalized : normalized.substring(0, 900);
    }
}
