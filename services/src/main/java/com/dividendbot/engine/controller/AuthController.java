package com.dividendbot.engine.controller;

import com.dividendbot.engine.config.JwtProvider;
import com.dividendbot.engine.domain.entity.AccountType;
import com.dividendbot.engine.domain.entity.User;
import com.dividendbot.engine.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    /**
     * 일반 회원가입
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");
        String nickname = body.getOrDefault("nickname", "사용자");

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "이메일과 비밀번호는 필수입니다"));
        }

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("error", "이미 등록된 이메일입니다"));
        }

        User user = User.builder()
                .email(email)
                .password(hashPassword(password))
                .nickname(nickname)
                .accountType(AccountType.GENERAL)
                .role("USER")
                .build();

        userRepository.save(user);
        String token = jwtProvider.generateToken(user.getId(), user.getRole());

        log.info("New user registered: {}", email);
        return ResponseEntity.ok(Map.of(
                "token", token,
                "userId", user.getId().toString(),
                "email", email,
                "nickname", nickname,
                "role", user.getRole()
        ));
    }

    /**
     * 일반 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "이메일과 비밀번호를 입력하세요"));
        }

        return userRepository.findByEmail(email)
                .filter(user -> user.getPassword().equals(hashPassword(password)))
                .map(user -> {
                    String token = jwtProvider.generateToken(user.getId(), user.getRole());
                    return ResponseEntity.ok(Map.<String, Object>of(
                            "token", token,
                            "userId", user.getId().toString(),
                            "email", user.getEmail(),
                            "nickname", user.getNickname() != null ? user.getNickname() : "",
                            "role", user.getRole()
                    ));
                })
                .orElse(ResponseEntity.status(401).body(Map.of("error", "이메일 또는 비밀번호가 올바르지 않습니다")));
    }

    /**
     * 카카오 로그인 (기존)
     */
    @PostMapping("/kakao")
    public ResponseEntity<Map<String, Object>> kakaoLogin(@RequestBody Map<String, String> body) {
        String kakaoUserId = body.getOrDefault("kakaoUserId", "kakao_" + System.currentTimeMillis());

        User user = userRepository.findByKakaoUserId(kakaoUserId)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .kakaoUserId(kakaoUserId)
                            .nickname("카카오사용자")
                            .accountType(AccountType.GENERAL)
                            .role("USER")
                            .build();
                    return userRepository.save(newUser);
                });

        String token = jwtProvider.generateToken(user.getId(), user.getRole());
        return ResponseEntity.ok(Map.of(
                "token", token,
                "userId", user.getId().toString(),
                "nickname", user.getNickname() != null ? user.getNickname() : "",
                "role", user.getRole()
        ));
    }

    /**
     * 토큰 검증
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "인증이 필요합니다"));
        }

        String token = authHeader.substring(7);
        if (!jwtProvider.validateToken(token)) {
            return ResponseEntity.status(401).body(Map.of("error", "유효하지 않은 토큰입니다"));
        }

        var userId = jwtProvider.getUserIdFromToken(token);
        return userRepository.findById(userId)
                .map(user -> ResponseEntity.ok(Map.<String, Object>of(
                        "userId", user.getId().toString(),
                        "email", user.getEmail() != null ? user.getEmail() : "",
                        "nickname", user.getNickname() != null ? user.getNickname() : "",
                        "role", user.getRole()
                )))
                .orElse(ResponseEntity.status(401).body(Map.of("error", "사용자를 찾을 수 없습니다")));
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Password hashing failed", e);
        }
    }
}
