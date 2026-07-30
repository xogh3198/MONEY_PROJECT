package com.dividendbot.news.promotion.plan;

import com.dividendbot.news.promotion.analysis.WebsiteAnalysisService;
import com.dividendbot.news.promotion.channel.Channel;
import com.dividendbot.news.promotion.channel.ChannelCatalog;
import com.dividendbot.news.promotion.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.dividendbot.news.promotion.plan.PromotionPlanModels.ChannelRecommendation;
import static com.dividendbot.news.promotion.plan.PromotionPlanModels.CreateRequest;
import static com.dividendbot.news.promotion.plan.PromotionPlanModels.PromotionAction;
import static com.dividendbot.news.promotion.plan.PromotionPlanModels.Response;

@Service
public class PromotionPlanService {

    private final WebsiteAnalysisService analysisService;
    private final RecommendationService recommendationService;
    private final CostScenarioService costScenarioService;
    private final ChannelCatalog channelCatalog;
    private final Map<String, Response> plans = new ConcurrentHashMap<>();

    public PromotionPlanService(
            WebsiteAnalysisService analysisService,
            RecommendationService recommendationService,
            CostScenarioService costScenarioService,
            ChannelCatalog channelCatalog
    ) {
        this.analysisService = analysisService;
        this.recommendationService = recommendationService;
        this.costScenarioService = costScenarioService;
        this.channelCatalog = channelCatalog;
    }

    public Response create(CreateRequest request) {
        analysisService.getRequired(request.analysisId());
        List<ChannelRecommendation> recommendations = recommendationService.recommend(request);
        List<PromotionAction> actions = createActions(recommendations, request);
        String planId = UUID.randomUUID().toString();

        Response response = new Response(
                planId,
                request.analysisId(),
                "%s 고객의 ‘%s’ 행동을 만들기 위해 검색 의도부터 검증하세요."
                        .formatted(request.targetAudience(), request.goal()),
                recommendations,
                costScenarioService.calculate(request.monthlyBudget()),
                actions,
                List.of(
                        "외부 페이지 본문을 아직 수집하지 않아 사용자가 입력한 고객·목표를 기준으로 계산했습니다.",
                        "비용은 확정 견적이 아닌 계획 범위이며 내부 운영시간을 시간당 20,000원으로 환산했습니다.",
                        "예상 성과를 보장하지 않으며 실제 게시·측정 결과로 다음 계획을 보정해야 합니다."
                ),
                Instant.now()
        );
        plans.put(planId, response);
        return response;
    }

    public Response getRequired(String planId) {
        Response response = plans.get(planId);
        if (response == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "PLAN_NOT_FOUND",
                    "홍보계획을 찾을 수 없습니다."
            );
        }
        return response;
    }

    private List<PromotionAction> createActions(
            List<ChannelRecommendation> recommendations,
            CreateRequest brief
    ) {
        return recommendations.stream()
                .map(recommendation -> {
                    Channel channel = channelCatalog.findById(recommendation.channelId())
                            .orElseThrow();
                    long cost = Math.min(
                            recommendation.estimatedCostMin(),
                            Math.max(0, brief.monthlyBudget() / 5)
                    );
                    return new PromotionAction(
                            UUID.randomUUID().toString(),
                            "%s 첫 콘텐츠 만들기".formatted(channel.name()),
                            recommendation.reason(),
                            channel.name(),
                            channel.contentType(),
                            "%s: %s".formatted(brief.targetAudience(), channel.actionVerb()),
                            List.of("핵심 상품 사실", "고객 질문 3개", "사이트 연결 URL", "게시 전 표현 검토"),
                            cost,
                            channel.estimatedHours(),
                            "시작 전"
                    );
                })
                .toList();
    }
}

