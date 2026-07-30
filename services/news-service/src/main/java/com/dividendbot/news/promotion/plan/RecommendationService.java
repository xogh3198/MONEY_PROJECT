package com.dividendbot.news.promotion.plan;

import com.dividendbot.news.promotion.channel.Channel;
import com.dividendbot.news.promotion.channel.ChannelCatalog;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import static com.dividendbot.news.promotion.plan.PromotionPlanModels.ChannelRecommendation;
import static com.dividendbot.news.promotion.plan.PromotionPlanModels.CreateRequest;

@Service
public class RecommendationService {

    private final ChannelCatalog channelCatalog;

    public RecommendationService(ChannelCatalog channelCatalog) {
        this.channelCatalog = channelCatalog;
    }

    public List<ChannelRecommendation> recommend(CreateRequest brief) {
        List<RankedChannel> ranked = channelCatalog.all().stream()
                .map(channel -> new RankedChannel(channel, score(channel, brief)))
                .sorted(Comparator.comparingInt(RankedChannel::score).reversed())
                .limit(5)
                .toList();

        AtomicInteger order = new AtomicInteger();
        return ranked.stream()
                .map(item -> toRecommendation(item.channel(), item.score(), order.getAndIncrement(), brief))
                .toList();
    }

    private int score(Channel channel, CreateRequest brief) {
        int score = channel.baseScore();
        if (channel.goals().contains(brief.goal())) {
            score += 12;
        }
        if (brief.monthlyBudget() == 0 && channel.organic()) {
            score += 10;
        }
        if (brief.monthlyBudget() < channel.estimatedCostMin()) {
            score -= 10;
        }

        List<String> capabilities = brief.productionCapabilities() == null
                ? List.of()
                : brief.productionCapabilities();
        String capabilityText = String.join(" ", capabilities).toLowerCase(Locale.ROOT);
        if (channel.contentType().contains("영상")
                && !capabilityText.contains("촬영")
                && !capabilityText.contains("편집")) {
            score -= 7;
        }
        if (channel.contentType().contains("글") && capabilityText.contains("글")) {
            score += 5;
        }
        return Math.max(0, Math.min(100, score));
    }

    private ChannelRecommendation toRecommendation(
            Channel channel,
            int score,
            int order,
            CreateRequest brief
    ) {
        String priority = order < 2 ? "우선 실행" : order < 4 ? "실험" : "보류";
        String budgetReason = brief.monthlyBudget() == 0
                ? "현금 지출 없이 시작할 수 있는 범위를 우선 계산했습니다."
                : "월 실험 예산 안에서 작은 검증이 가능한지 반영했습니다.";
        String reason = "%s 목표의 %s 단계에 적합합니다. %s"
                .formatted(brief.goal(), channel.funnelStage(), budgetReason);

        String warning = channel.estimatedCostMin() > brief.monthlyBudget()
                ? "현재 예산보다 최소 실험비가 높아 유료 집행 전 재검토가 필요합니다."
                : null;

        return new ChannelRecommendation(
                channel.id(),
                channel.name(),
                priority,
                score,
                reason,
                channel.funnelStage(),
                channel.estimatedCostMin(),
                channel.estimatedCostMax(),
                channel.estimatedHours(),
                channel.confidence(),
                warning,
                channel.sourceUrl(),
                channel.verifiedAt()
        );
    }

    private record RankedChannel(Channel channel, int score) {
    }
}

