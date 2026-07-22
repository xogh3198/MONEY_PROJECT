package com.dividendbot.news.service.engagement;

import com.dividendbot.news.domain.entity.ExternalMetricStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StructuredDataEngagementParserTest {
    private final StructuredDataEngagementParser parser = new StructuredDataEngagementParser(new ObjectMapper());

    @Test
    void readsSchemaOrgInteractionCounters() {
        String html = """
                <html><head><script type="application/ld+json">
                {
                  "@type": "NewsArticle",
                  "interactionStatistic": [
                    {"@type":"InteractionCounter","interactionType":"https://schema.org/ViewAction","userInteractionCount":"12345"},
                    {"@type":"InteractionCounter","interactionType":"https://schema.org/CommentAction","userInteractionCount":42},
                    {"@type":"InteractionCounter","interactionType":{"@type":"LikeAction"},"userInteractionCount":"300"},
                    {"@type":"InteractionCounter","interactionType":"https://schema.org/DislikeAction","userInteractionCount":"7"}
                  ]
                }
                </script></head></html>
                """;

        ExternalEngagementMetrics metrics = parser.parse(Jsoup.parse(html));

        assertThat(metrics.views()).isEqualTo(12_345L);
        assertThat(metrics.comments()).isEqualTo(42L);
        assertThat(metrics.positive()).isEqualTo(300L);
        assertThat(metrics.negative()).isEqualTo(7L);
        assertThat(metrics.status()).isEqualTo(ExternalMetricStatus.AVAILABLE);
    }

    @Test
    void usesMaximumInsteadOfDoubleCountingDuplicateBlocks() {
        String html = """
                <script type="application/ld+json">
                [{"@type":"NewsArticle","commentCount":"12"},{"@type":"NewsArticle","commentCount":"12"},{"@type":"NewsArticle","commentCount":"9"}]
                </script>
                """;
        assertThat(parser.parse(Jsoup.parse(html)).comments()).isEqualTo(12L);
    }

    @Test
    void ignoresCountersOwnedByUnrelatedOrganization() {
        String html = """
                <script type="application/ld+json">
                {"@graph":[
                  {"@type":"Organization","interactionStatistic":{"interactionType":"LikeAction","userInteractionCount":999999}},
                  {"@type":"NewsArticle","commentCount":12}
                ]}
                </script>
                """;

        ExternalEngagementMetrics metrics = parser.parse(Jsoup.parse(html));

        assertThat(metrics.comments()).isEqualTo(12L);
        assertThat(metrics.positive()).isNull();
    }

    @Test
    void reportsUnsupportedWhenNoPublicCountsExist() {
        ExternalEngagementMetrics metrics = parser.parse(Jsoup.parse("<html><head><title>기사</title></head></html>"));
        assertThat(metrics.hasAnyCount()).isFalse();
        assertThat(metrics.status()).isEqualTo(ExternalMetricStatus.NOT_SUPPORTED);
    }
}
