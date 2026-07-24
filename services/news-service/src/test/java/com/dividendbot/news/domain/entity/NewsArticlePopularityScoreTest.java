package com.dividendbot.news.domain.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NewsArticlePopularityScoreTest {

    @Test
    void expandsSearchInterestIntoRankingPointsWithoutChangingViews() {
        NewsArticle article = NewsArticle.builder()
                .title("금리 기사")
                .category(NewsCategory.RATE)
                .viewCount(12)
                .commentCount(2)
                .positiveVotes(3)
                .negativeVotes(1)
                .externalTrendScore(7)
                .externalEngagementScore(11)
                .externalSearchInterest(60)
                .build();

        assertThat(article.getViewCount()).isEqualTo(12);
        assertThat(article.getSearchInterestPopularityScore()).isEqualTo(6_000);
        assertThat(article.getIntegratedViewCount()).isEqualTo(6_012);
        assertThat(article.getPopularityScore()).isEqualTo(6_059);
    }

    @Test
    void missingSearchInterestAddsNoSyntheticPoints() {
        NewsArticle article = NewsArticle.builder()
                .title("환율 기사")
                .category(NewsCategory.FOREX)
                .viewCount(3)
                .build();

        assertThat(article.getSearchInterestPopularityScore()).isZero();
        assertThat(article.getIntegratedViewCount()).isEqualTo(3);
        assertThat(article.getPopularityScore()).isEqualTo(3);
    }
}
