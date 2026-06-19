package com.dividendbot.news.controller;

import com.dividendbot.news.domain.entity.VoteType;
import com.dividendbot.news.service.VoteService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/forum/posts/{postId}/vote")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VoteController {

    private final VoteService voteService;

    @PostMapping
    public ResponseEntity<VoteResponse> toggleVote(
            @PathVariable UUID postId,
            @RequestBody VoteRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = (UUID) httpRequest.getAttribute("userId");
        VoteType voteType = VoteType.valueOf(request.voteType().toUpperCase());
        VoteService.VoteResult result = voteService.toggleVote(postId, userId, voteType);
        return ResponseEntity.ok(new VoteResponse(
                result.userVote(), result.likeCount(), result.dislikeCount()));
    }

    public record VoteRequest(String voteType) {}

    public record VoteResponse(String userVote, int likeCount, int dislikeCount) {}
}
