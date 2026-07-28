package com.dividendbot.news.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "video_render_jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VideoRenderJob {

    @Id
    private UUID id;

    @Column(name = "experiment_id", nullable = false, length = 160)
    private String experimentId;

    @Column(nullable = false, length = 240)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VideoRenderQuality quality;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VideoRenderStatus status;

    @Column(nullable = false, length = 80)
    private String stage;

    @Column(nullable = false)
    private int progress;

    @Column(name = "voice_provider", length = 40)
    private String voiceProvider;

    @Column(name = "output_file_name", length = 255)
    private String outputFileName;

    @Column(name = "duration_seconds")
    private Double durationSeconds;

    @Column(name = "asset_credits", columnDefinition = "TEXT")
    private String assetCredits;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public static VideoRenderJob queued(
            UUID id,
            String experimentId,
            String title,
            VideoRenderQuality quality,
            String voiceProvider
    ) {
        VideoRenderJob job = new VideoRenderJob();
        job.id = id;
        job.experimentId = trim(experimentId, 160);
        job.title = trim(title, 240);
        job.quality = quality;
        job.status = VideoRenderStatus.QUEUED;
        job.stage = "대기 중";
        job.progress = 0;
        job.voiceProvider = trim(voiceProvider, 40);
        job.createdAt = LocalDateTime.now();
        job.updatedAt = job.createdAt;
        return job;
    }

    public void markRendering(String stage, int progress) {
        this.status = VideoRenderStatus.RENDERING;
        this.stage = trim(stage, 80);
        this.progress = Math.max(0, Math.min(99, progress));
        this.updatedAt = LocalDateTime.now();
    }

    public void markCompleted(String outputFileName, double durationSeconds, String assetCredits) {
        this.status = VideoRenderStatus.COMPLETED;
        this.stage = "완료";
        this.progress = 100;
        this.outputFileName = trim(outputFileName, 255);
        this.durationSeconds = Math.max(0, durationSeconds);
        this.assetCredits = assetCredits;
        this.errorMessage = null;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = this.completedAt;
    }

    public void markFailed(String errorMessage) {
        this.status = VideoRenderStatus.FAILED;
        this.stage = "실패";
        this.errorMessage = trim(errorMessage == null ? "알 수 없는 렌더 오류" : errorMessage, 1000);
        this.completedAt = LocalDateTime.now();
        this.updatedAt = this.completedAt;
    }

    private static String trim(String value, int limit) {
        String normalized = value == null ? "" : value.replaceAll("[\\r\\n\\t]+", " ").trim();
        return normalized.length() <= limit ? normalized : normalized.substring(0, limit);
    }
}
