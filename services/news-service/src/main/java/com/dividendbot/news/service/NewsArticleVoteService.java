package com.dividendbot.news.service;

import com.dividendbot.news.domain.entity.NewsArticle;
import com.dividendbot.news.domain.entity.NewsArticleVote;
import com.dividendbot.news.domain.entity.VoteType;
import com.dividendbot.news.domain.repository.NewsArticleRepository;
import com.dividendbot.news.domain.repository.NewsArticleVoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class NewsArticleVoteService {

    private final NewsArticleVoteRepository voteRepository;
    private final NewsArticleRepository articleRepository;

    public VoteResult toggleVote(UUID articleId, UUID userId, VoteType voteType) {
        NewsArticle article = articleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("기사를 찾을 수 없습니다: " + articleId));

        Optional<NewsArticleVote> existingVote = voteRepository.findByArticleIdAndUserId(articleId, userId);
        String userVote;

        if (existingVote.isEmpty()) {
            voteRepository.save(NewsArticleVote.builder()
                    .articleId(articleId)
                    .userId(userId)
                    .voteType(voteType)
                    .build());
            increment(article, voteType);
            userVote = voteType.name();
        } else {
            NewsArticleVote vote = existingVote.get();
            if (vote.getVoteType() == voteType) {
                voteRepository.delete(vote);
                decrement(article, voteType);
                userVote = null;
            } else {
                decrement(article, vote.getVoteType());
                increment(article, voteType);
                vote.changeVoteType(voteType);
                voteRepository.save(vote);
                userVote = voteType.name();
            }
        }

        articleRepository.save(article);
        return new VoteResult(userVote, article.getPositiveVotes(), article.getNegativeVotes());
    }

    private void increment(NewsArticle article, VoteType type) {
        if (type == VoteType.LIKE) article.addPositiveVote();
        else article.addNegativeVote();
    }

    private void decrement(NewsArticle article, VoteType type) {
        if (type == VoteType.LIKE) article.removePositiveVote();
        else article.removeNegativeVote();
    }

    public record VoteResult(String userVote, int positiveVotes, int negativeVotes) {}
}
