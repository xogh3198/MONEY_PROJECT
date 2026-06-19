package com.dividendbot.engine.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "alert_timing_d7", nullable = false)
    @Builder.Default
    private boolean alertTimingD7 = false;

    @Column(name = "alert_timing_d3", nullable = false)
    @Builder.Default
    private boolean alertTimingD3 = true;

    @Column(name = "alert_timing_d1", nullable = false)
    @Builder.Default
    private boolean alertTimingD1 = true;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * 알림 설정 필드들을 업데이트합니다.
     */
    public void updatePreferences(boolean enabled, boolean alertTimingD7, boolean alertTimingD3, boolean alertTimingD1) {
        this.enabled = enabled;
        this.alertTimingD7 = alertTimingD7;
        this.alertTimingD3 = alertTimingD3;
        this.alertTimingD1 = alertTimingD1;
        this.updatedAt = LocalDateTime.now();
    }
}
