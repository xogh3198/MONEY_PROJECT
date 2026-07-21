package com.dividendbot.news.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "news_article_votes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"article_id", "user_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NewsArticleVote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "article_id", nullable = false)
    private UUID articleId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "vote_type", nullable = false, length = 10)
    private VoteType voteType;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public void changeVoteType(VoteType newType) {
        this.voteType = newType;
    }
}
