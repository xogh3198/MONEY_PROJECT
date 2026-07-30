package com.dividendbot.news.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class GrowthAnalyticsAccessGuard {

    private final String accessKey;

    public GrowthAnalyticsAccessGuard(@Value("${video.render.access-key:}") String accessKey) {
        this.accessKey = accessKey;
    }

    public void requireAuthorized(String providedKey) {
        if (accessKey == null || accessKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "운영 분석 접근 키가 설정되지 않았습니다.");
        }
        byte[] expected = accessKey.getBytes(StandardCharsets.UTF_8);
        byte[] actual = (providedKey == null ? "" : providedKey).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "운영 분석 인증에 실패했습니다.");
        }
    }
}
