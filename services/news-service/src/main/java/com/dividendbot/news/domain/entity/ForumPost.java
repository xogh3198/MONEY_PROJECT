package com.dividendbot.news.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "forum_posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ForumPost {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(nullable = false, length = 50) private String nickname;
    @Column(nullable = false, length = 100) private String title;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PostCategory category;
    @Column(name = "view_count", nullable = false) @Builder.Default private int viewCount = 0;
    @Column(name = "like_count", nullable = false) @Builder.Default private int likeCount = 0;
    @Column(name = "dislike_count", nullable = false) @Builder.Default private int dislikeCount = 0;
    @Column(name = "comment_count", nullable = false) @Builder.Default private int commentCount = 0;
    @Column(name = "created_at", nullable = false) @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    public void updatePost(String title, String content) {
        this.title = title;
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }
    public void incrementViewCount() { this.viewCount++; }
    public void incrementCommentCount() { this.commentCount++; }
    public void decrementCommentCount() { if (this.commentCount > 0) this.commentCount--; }
    public void incrementLikeCount() { this.likeCount++; }
    public void decrementLikeCount() { if (this.likeCount > 0) this.likeCount--; }
    public void incrementDislikeCount() { this.dislikeCount++; }
    public void decrementDislikeCount() { if (this.dislikeCount > 0) this.dislikeCount--; }
}
