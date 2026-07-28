package com.dividendbot.news.dto;

import com.dividendbot.news.domain.entity.VideoRenderJob;
import com.dividendbot.news.domain.entity.VideoRenderQuality;
import com.dividendbot.news.domain.entity.VideoRenderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record VideoRenderJobResponse(
        UUID id,
        String experimentId,
        String title,
        VideoRenderQuality quality,
        VideoRenderStatus status,
        String stage,
        int progress,
        String voiceProvider,
        Double durationSeconds,
        String assetCredits,
        String errorMessage,
        String filePath,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt
) {
    public static VideoRenderJobResponse from(VideoRenderJob job) {
        String filePath = job.getStatus() == VideoRenderStatus.COMPLETED
                ? "/api/content-videos/" + job.getId() + "/file"
                : null;
        return new VideoRenderJobResponse(
                job.getId(),
                job.getExperimentId(),
                job.getTitle(),
                job.getQuality(),
                job.getStatus(),
                job.getStage(),
                job.getProgress(),
                job.getVoiceProvider(),
                job.getDurationSeconds(),
                job.getAssetCredits(),
                job.getErrorMessage(),
                filePath,
                job.getCreatedAt(),
                job.getUpdatedAt(),
                job.getCompletedAt()
        );
    }
}
