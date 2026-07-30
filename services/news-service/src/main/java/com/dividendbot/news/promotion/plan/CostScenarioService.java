package com.dividendbot.news.promotion.plan;

import org.springframework.stereotype.Service;

import java.util.List;

import static com.dividendbot.news.promotion.plan.PromotionPlanModels.CostScenario;

@Service
public class CostScenarioService {

    private static final long HOURLY_OPERATION_COST = 20_000;

    public List<CostScenario> calculate(long monthlyBudget) {
        long savingMedia = roundToTenThousand(monthlyBudget * 40 / 100);
        long standardMedia = roundToTenThousand(monthlyBudget * 60 / 100);
        long standardProduction = roundToTenThousand(monthlyBudget * 20 / 100);

        return List.of(
                scenario(
                        "free",
                        "무료·직접 운영",
                        "아이디어와 메시지 검증",
                        0,
                        0,
                        6 * HOURLY_OPERATION_COST,
                        0,
                        0
                ),
                scenario(
                        "saving",
                        "절약형",
                        "직접 제작 + 소액 채널 실험",
                        savingMedia,
                        0,
                        8 * HOURLY_OPERATION_COST,
                        20_000,
                        roundToTenThousand(savingMedia / 10)
                ),
                scenario(
                        "standard",
                        "표준형",
                        "반복 콘텐츠 + 검색·소셜 검증",
                        standardMedia,
                        standardProduction,
                        12 * HOURLY_OPERATION_COST,
                        50_000,
                        roundToTenThousand((standardMedia + standardProduction) / 10)
                )
        );
    }

    private CostScenario scenario(
            String id,
            String name,
            String description,
            long media,
            long production,
            long operation,
            long tools,
            long contingency
    ) {
        return new CostScenario(
                id,
                name,
                description,
                media,
                production,
                operation,
                tools,
                contingency,
                media + production + operation + tools + contingency
        );
    }

    private long roundToTenThousand(long amount) {
        return Math.round(amount / 10_000.0) * 10_000;
    }
}

