package com.dividendbot.news.promotion.plan;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;
import java.util.List;

public final class PromotionPlanModels {

    private PromotionPlanModels() {
    }

    public record CreateRequest(
            @NotBlank(message = "분석 ID가 필요합니다.")
            String analysisId,
            @NotBlank(message = "목표를 선택해 주세요.")
            String goal,
            @NotBlank(message = "핵심 고객을 입력해 주세요.")
            String targetAudience,
            @NotBlank(message = "대상 지역을 입력해 주세요.")
            String targetRegion,
            @NotNull(message = "월 예산을 입력해 주세요.")
            @PositiveOrZero(message = "월 예산은 0원 이상이어야 합니다.")
            Long monthlyBudget,
            List<String> productionCapabilities
    ) {
    }

    public record ChannelRecommendation(
            String channelId,
            String name,
            String priority,
            int score,
            String reason,
            String funnelStage,
            long estimatedCostMin,
            long estimatedCostMax,
            int estimatedHours,
            String confidence,
            String warning,
            String sourceUrl,
            String verifiedAt
    ) {
    }

    public record CostScenario(
            String id,
            String name,
            String description,
            long mediaCost,
            long productionCost,
            long operationCost,
            long toolCost,
            long contingency,
            long totalCost
    ) {
    }

    public record PromotionAction(
            String id,
            String title,
            String reason,
            String channelName,
            String contentType,
            String hook,
            List<String> preparation,
            long estimatedCost,
            int estimatedHours,
            String status
    ) {
    }

    public record Response(
            String planId,
            String analysisId,
            String strategySummary,
            List<ChannelRecommendation> recommendations,
            List<CostScenario> costScenarios,
            List<PromotionAction> actions,
            List<String> assumptions,
            Instant generatedAt
    ) {
    }
}

