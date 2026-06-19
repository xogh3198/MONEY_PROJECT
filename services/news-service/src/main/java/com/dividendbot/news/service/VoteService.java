package com.dividendbot.news.service;

import com.dividendbot.news.domain.entity.ForumPost;
import com.dividendbot.news.domain.entity.Vote;
import com.dividendbot.news.domain.entity.VoteType;
import com.dividendbot.news.domain.repository.ForumPostRepository;
import com.dividendbot.news.domain.repository.VoteRepository;
import com.dividendbot.news.exception.ForumPostNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class VoteService {

    private final VoteRepository voteRepository;
    private final ForumPostRepository forumPostRepository;

    public VoteResult toggleVote(UUID postId, UUID userId, VoteType voteType) {
        ForumPost post = forumPostRepository.findById(postId)
                .orElseThrow(() -> new ForumPostNotFoundException(postId));

        Optional<Vote> existingVote = voteRepository.findByUserIdAndPostId(userId, postId);

        String userVote;

        if (existingVote.isEmpty()) {
            // No existing vote - create new vote
            Vote vote = Vote.builder()
                    .userId(userId)
                    .postId(postId)
                    .voteType(voteType)
                    .build();
            voteRepository.save(vote);

            if (voteType == VoteType.LIKE) {
                post.incrementLikeCount();
            } else {
                post.incrementDislikeCount();
            }
            userVote = voteType.name();
        } else {
            Vote vote = existingVote.get();

            if (vote.getVoteType() == voteType) {
                // Same type - cancel vote
                voteRepository.delete(vote);
                if (voteType == VoteType.LIKE) {
                    post.decrementLikeCount();
                } else {
                    post.decrementDislikeCount();
                }
                userVote = null;
            } else {
                // Different type - change vote
                if (vote.getVoteType() == VoteType.LIKE) {
                    post.decrementLikeCount();
                    post.incrementDislikeCount();
                } else {
                    post.decrementDislikeCount();
                    post.incrementLikeCount();
                }
                vote.changeVoteType(voteType);
                voteRepository.save(vote);
                userVote = voteType.name();
            }
        }

        forumPostRepository.save(post);

        return new VoteResult(userVote, post.getLikeCount(), post.getDislikeCount());
    }

    public record VoteResult(String userVote, int likeCount, int dislikeCount) {}
}
