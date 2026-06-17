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

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public void incrementViewCount() { this.viewCount++; }
    public void addPositiveVote() { this.positiveVotes++; }
    public void addNegativeVote() { this.negativeVotes++; }
    public void incrementCommentCount() { this.commentCount++; }
}
