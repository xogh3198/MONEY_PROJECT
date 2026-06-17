package com.dividendbot.engine.controller;

import com.dividendbot.engine.domain.entity.AccountType;
import com.dividendbot.engine.domain.entity.User;
import com.dividendbot.engine.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * 카카오 OAuth 로그인/회원가입
 * - 프론트에서 카카오 로그인 후 인가코드 전달
 * - 백엔드에서 Access Token 교환 → 사용자 정보 조회 → JWT 발급
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;

    @Value("${kakao.oauth.client-id:}")
    private String kakaoClientId;

    @Value("${kakao.oauth.redirect-uri:http://localhost:3000/auth/callback}")
    private String redirectUri;

    /**
     * 카카오 OAuth 콜백 — 인가코드로 토큰 교환 후 로그인/회원가입 처리
     */
    @PostMapping("/kakao")
    public ResponseEntity<Map<String, Object>> kakaoLogin(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "인가코드가 필요합니다"));
        }

        try {
            // 1. 카카오에서 Access Token 획득
            String accessToken = exchangeToken(code);

            // 2. 사용자 정보 조회
            Map<String, Object> userInfo = getUserInfo(accessToken);
            String kakaoUserId = String.valueOf(userInfo.get("id"));

            // 3. DB에서 사용자 조회 또는 신규 생성
            User user = userRepository.findByKakaoUserId(kakaoUserId)
                    .orElseGet(() -> {
                        User newUser = User.builder()
                                .kakaoUserId(kakaoUserId)
                                .accountType(AccountType.GENERAL)
                                .build();
                        log.info("New user created: kakaoId={}", kakaoUserId);
                        return userRepository.save(newUser);
                    });

            // 4. JWT 발급 (MVP: 간단한 토큰 반환)
            // TODO: 실제 JWT 라이브러리 (jjwt) 적용
            return ResponseEntity.ok(Map.of(
                    "token", "jwt_" + user.getId().toString(),
                    "userId", user.getId().toString(),
                    "accountType", user.getAccountType().name()
            ));
        } catch (Exception e) {
            log.error("Kakao login failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "로그인 처리 실패"));
        }
    }

    private String exchangeToken(String code) {
        if (kakaoClientId.isBlank()) {
            log.warn("Kakao client ID not configured, using mock token");
            return "MOCK_ACCESS_TOKEN";
        }

        WebClient client = WebClient.create("https://kauth.kakao.com");
        Map response = client.post()
                .uri("/oauth/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue(String.format(
                        "grant_type=authorization_code&client_id=%s&redirect_uri=%s&code=%s",
                        kakaoClientId, redirectUri, code))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return (String) response.get("access_token");
    }

    private Map<String, Object> getUserInfo(String accessToken) {
        if ("MOCK_ACCESS_TOKEN".equals(accessToken)) {
            return Map.of("id", "mock_user_" + System.currentTimeMillis());
        }

        WebClient client = WebClient.create("https://kapi.kakao.com");
        return client.get()
                .uri("/v2/user/me")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }
}
