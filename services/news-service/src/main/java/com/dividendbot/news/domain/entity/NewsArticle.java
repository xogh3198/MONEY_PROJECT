package com.dividendbot.news.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "news_articles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NewsArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(name = "source_name", length = 50)
    private String sourceName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NewsCategory category;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private NewsSentiment sentiment = NewsSentiment.NEUTRAL;

    @Column(name = "view_count")
    @Builder.Default
    private int viewCount = 0;

    @Column(name = "comment_count")
    @Builder.Default
    private int commentCount = 0;

    @Column(name = "positive_votes")
    @Builder.Default
    private int positiveVotes = 0;

    @Column(name = "negative_votes")
    @Builder.Default
    private int negativeVotes = 0;

    /**
     * 외부 플랫폼의 실시간 랭킹 신호입니다.
     * 실제 InvestBoard 조회수와 분리해 사용자 지표가 부풀려지지 않게 합니다.
     */
    @Column(name = "external_trend_score")
    @Builder.Default
    private Integer externalTrendScore = 0;

    /**
     * 원문 또는 공식 플랫폼 API가 공개한 참여 수치입니다.
     * InvestBoard 내부 조회/댓글/투표와 절대 합산하지 않습니다.
     */
    @Column(name = "external_view_count")
    private Long externalViewCount;

    @Column(name = "external_comment_count")
    private Long externalCommentCount;

    @Column(name = "external_positive_count")
    private Long externalPositiveCount;

    @Column(name = "external_negative_count")
    private Long externalNegativeCount;

    @Column(name = "external_engagement_score", nullable = false)
    @Builder.Default
    private Integer externalEngagementScore = 0;

    @Column(name = "external_metric_provider", length = 40)
    private String externalMetricProvider;

    @Enumerated(EnumType.STRING)
    @Column(name = "external_metric_status", nullable = false, length = 30)
    @Builder.Default
    private ExternalMetricStatus externalMetricStatus = ExternalMetricStatus.PENDING;

    @Column(name = "external_metrics_updated_at")
    private LocalDateTime externalMetricsUpdatedAt;

    /** 네이버 DataLab 공식 API가 제공하는 기사 카테고리 키워드 그룹의 0~100 상대 검색 관심도입니다. */
    @Column(name = "external_search_interest")
    private Integer externalSearchInterest;

    @Column(name = "external_search_interest_source", length = 40)
    private String externalSearchInterestSource;

    @Column(name = "external_search_interest_updated_at")
    private LocalDateTime externalSearchInterestUpdatedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public void incrementViewCount() { this.viewCount++; }
    public void addPositiveVote() { this.positiveVotes++; }
    public void addNegativeVote() { this.negativeVotes++; }
    public void removePositiveVote() { this.positiveVotes = Math.max(0, this.positiveVotes - 1); }
    public void removeNegativeVote() { this.negativeVotes = Math.max(0, this.negativeVotes - 1); }
    public void incrementCommentCount() { this.commentCount++; }

    public void updateExternalTrendScore(int score) {
        this.externalTrendScore = Math.max(0, score);
    }

    public void updateExternalMetrics(
            Long views,
            Long comments,
            Long positive,
            Long negative,
            int engagementScore,
            String provider,
            ExternalMetricStatus status,
            LocalDateTime updatedAt
    ) {
        this.externalViewCount = sanitizeCount(views);
        this.externalCommentCount = sanitizeCount(comments);
        this.externalPositiveCount = sanitizeCount(positive);
        this.externalNegativeCount = sanitizeCount(negative);
        this.externalEngagementScore = Math.max(0, engagementScore);
        this.externalMetricProvider = provider;
        this.externalMetricStatus = status == null ? ExternalMetricStatus.FETCH_ERROR : status;
        this.externalMetricsUpdatedAt = updatedAt;
    }

    public void updateExternalSearchInterest(int score, String source, LocalDateTime updatedAt) {
        this.externalSearchInterest = Math.max(0, Math.min(100, score));
        this.externalSearchInterestSource = source;
        this.externalSearchInterestUpdatedAt = updatedAt;
    }

    public void migrateLegacyRankingViews() {
        int currentTrendScore = this.externalTrendScore == null ? 0 : this.externalTrendScore;
        this.externalTrendScore = Math.max(currentTrendScore, Math.min(this.viewCount, 200));
        this.viewCount = 0;
    }

    private Long sanitizeCount(Long value) {
        return value == null ? null : Math.max(0L, value);
    }
}
