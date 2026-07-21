package com.dividendbot.news.controller;

import com.dividendbot.news.domain.entity.VoteType;
import com.dividendbot.news.service.NewsArticleVoteService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/forum/articles/{articleId}/vote")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NewsArticleVoteController {

    private final NewsArticleVoteService voteService;

    @PostMapping
    public ResponseEntity<VoteResponse> toggleVote(
            @PathVariable UUID articleId,
            @RequestBody VoteRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId");
        VoteType voteType = VoteType.valueOf(request.voteType().toUpperCase());
        NewsArticleVoteService.VoteResult result = voteService.toggleVote(articleId, userId, voteType);
        return ResponseEntity.ok(new VoteResponse(
                result.userVote(), result.positiveVotes(), result.negativeVotes()));
    }

    public record VoteRequest(String voteType) {}
    public record VoteResponse(String userVote, int positiveVotes, int negativeVotes) {}
}
