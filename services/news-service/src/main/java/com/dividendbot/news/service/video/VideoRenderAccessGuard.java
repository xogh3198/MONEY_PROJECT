package com.dividendbot.news.service.video;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class VideoRenderAccessGuard {

    private final boolean enabled;
    private final String accessKey;

    public VideoRenderAccessGuard(
            @Value("${video.render.enabled:false}") boolean enabled,
            @Value("${video.render.access-key:}") String accessKey
    ) {
        this.enabled = enabled;
        this.accessKey = accessKey;
    }

    public void requireAuthorized(String providedKey) {
        if (!enabled || accessKey == null || accessKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "영상 렌더 기능이 아직 설정되지 않았습니다.");
        }
        byte[] expected = accessKey.getBytes(StandardCharsets.UTF_8);
        byte[] actual = (providedKey == null ? "" : providedKey).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "영상 렌더 인증에 실패했습니다.");
        }
    }

    public boolean configured() {
        return enabled && accessKey != null && !accessKey.isBlank();
    }
}
