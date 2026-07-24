package com.dividendbot.news.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "collector_run_states",
        uniqueConstraints = @UniqueConstraint(name = "uk_collector_run_states_name", columnNames = "collector_name")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CollectorRunState {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "collector_name", nullable = false, length = 60)
    private String collectorName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CollectorRunStatus status;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private boolean configured;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "last_success_at")
    private LocalDateTime lastSuccessAt;

    @Column(name = "last_failure_at")
    private LocalDateTime lastFailureAt;

    @Column(name = "last_duration_ms")
    private Long lastDurationMs;

    @Column(name = "processed_count")
    private Integer processedCount;

    @Column(name = "available_count")
    private Integer availableCount;

    @Column(length = 500)
    private String message;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static CollectorRunState create(String collectorName, boolean enabled, boolean configured) {
        LocalDateTime now = LocalDateTime.now();
        return CollectorRunState.builder()
                .collectorName(collectorName)
                .status(CollectorRunStatus.SKIPPED)
                .enabled(enabled)
                .configured(configured)
                .message("아직 실행 기록이 없습니다.")
                .updatedAt(now)
                .build();
    }

    public void markRunning(boolean enabled, boolean configured, LocalDateTime now) {
        this.status = CollectorRunStatus.RUNNING;
        this.enabled = enabled;
        this.configured = configured;
        this.lastAttemptAt = now;
        this.message = "수집을 실행하고 있습니다.";
        this.updatedAt = now;
    }

    public void markSuccess(
            int processedCount,
            int availableCount,
            long durationMs,
            String message,
            LocalDateTime now
    ) {
        this.status = CollectorRunStatus.SUCCESS;
        this.enabled = true;
        this.configured = true;
        this.lastSuccessAt = now;
        this.lastDurationMs = Math.max(0, durationMs);
        this.processedCount = Math.max(0, processedCount);
        this.availableCount = Math.max(0, availableCount);
        this.message = sanitize(message);
        this.updatedAt = now;
    }

    public void markSkipped(boolean enabled, boolean configured, String message, LocalDateTime now) {
        this.status = CollectorRunStatus.SKIPPED;
        this.enabled = enabled;
        this.configured = configured;
        this.lastAttemptAt = now;
        this.lastDurationMs = 0L;
        this.processedCount = 0;
        this.availableCount = 0;
        this.message = sanitize(message);
        this.updatedAt = now;
    }

    public void markFailed(long durationMs, String message, LocalDateTime now) {
        this.status = CollectorRunStatus.FAILED;
        this.lastFailureAt = now;
        this.lastDurationMs = Math.max(0, durationMs);
        this.message = sanitize(message);
        this.updatedAt = now;
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) return "상세 오류가 제공되지 않았습니다.";
        String normalized = value.replaceAll("[\\r\\n\\t]+", " ").trim();
        return normalized.length() <= 500 ? normalized : normalized.substring(0, 500);
    }
}
