package com.dividendbot.news.promotion.plan;

import com.dividendbot.news.promotion.channel.ChannelCatalog;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationServiceTest {

    private final RecommendationService service =
            new RecommendationService(new ChannelCatalog());

    @Test
    void returnsFiveRecommendationsSortedByScore() {
        var request = new PromotionPlanModels.CreateRequest(
                "analysis-id",
                "상담 문의",
                "지역 소상공인",
                "인천",
                300_000L,
                List.of("글 작성", "이미지 제작")
        );

        var result = service.recommend(request);

        assertThat(result).hasSize(5);
        assertThat(result)
                .extracting(PromotionPlanModels.ChannelRecommendation::score)
                .isSortedAccordingTo(java.util.Comparator.reverseOrder());
        assertThat(result.get(0).priority()).isEqualTo("우선 실행");
        assertThat(result).allSatisfy(item -> {
            assertThat(item.reason()).isNotBlank();
            assertThat(item.sourceUrl()).startsWith("https://");
            assertThat(item.verifiedAt()).isNotBlank();
        });
    }
}
