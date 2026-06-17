package com.dividendbot.gateway.controller;

import com.dividendbot.gateway.dto.kakao.KakaoSkillRequest;
import com.dividendbot.gateway.dto.kakao.KakaoSkillResponse;
import com.dividendbot.gateway.service.IntentRouterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/kakao")
@RequiredArgsConstructor
@Slf4j
public class KakaoSkillController {

    private final IntentRouterService intentRouterService;

    /**
     * 카카오 i 오픈빌더 스킬 엔드포인트
     * 응답 시간 5초 이내 필수
     */
    @PostMapping("/skill")
    public ResponseEntity<KakaoSkillResponse> handleSkill(@RequestBody KakaoSkillRequest request) {
        log.info("Kakao skill request: intent={}, user={}",
                request.getIntentName(),
                request.getUserId());

        KakaoSkillResponse response = intentRouterService.route(request);
        return ResponseEntity.ok(response);
    }
}
