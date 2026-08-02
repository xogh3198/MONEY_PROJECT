package com.dividendbot.news.dto;

import java.time.Instant;
import java.util.UUID;

public record AiSceneGenerationJobResponse(
        UUID id,
        int sceneOrder,
        String status,
        String stage,
        int progress,
        String provider,
        String assetRef,
        String mediaKind,
        String contentType,
        String fileName,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt
) {
}
