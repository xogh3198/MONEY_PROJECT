package com.dividendbot.news.promotion.analysis;

import com.dividendbot.news.promotion.common.ApiException;
import com.dividendbot.news.promotion.security.UrlSafetyValidator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebsiteAnalysisServiceTest {

    private final WebsiteAnalysisService service =
            new WebsiteAnalysisService(new UrlSafetyValidator());

    @Test
    void createsUrlSourceAndRemovesSensitiveQuery() {
        var result = service.create(new WebsiteAnalysisModels.CreateRequest(
                "URL",
                "https://example.com/product?token=secret",
                "",
                "",
                List.of()
        ));

        assertThat(result.sourceType()).isEqualTo("URL");
        assertThat(result.canonicalUrl()).isEqualTo("https://example.com/product");
        assertThat(result.title()).isEqualTo("example.com");
    }

    @Test
    void createsProductSourceWithoutUrl() {
        var result = service.create(new WebsiteAnalysisModels.CreateRequest(
                "PRODUCT",
                "",
                "동네 카페 정기구독",
                "매주 원두를 배송하는 지역 구독 서비스",
                List.of("https://example.com/reference")
        ));

        assertThat(result.sourceType()).isEqualTo("PRODUCT");
        assertThat(result.canonicalUrl()).isEmpty();
        assertThat(result.sourceSummary()).contains("원두");
        assertThat(result.evidence()).hasSize(2);
    }

    @Test
    void rejectsEmptyNonUrlSource() {
        assertThatThrownBy(() -> service.create(new WebsiteAnalysisModels.CreateRequest(
                "TEXT",
                "",
                "",
                "",
                List.of()
        ))).isInstanceOf(ApiException.class);
    }
}
