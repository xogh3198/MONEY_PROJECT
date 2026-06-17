package com.dividendbot.notification.service;

import com.dividendbot.notification.domain.entity.NotificationLog;
import com.dividendbot.notification.domain.repository.NotificationLogRepository;
import com.dividendbot.notification.dto.SendNotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalTime;

/**
 * 카카오 알림톡 발송 서비스
 * - 발송 시간: 08:00~21:00 (야간 발송 금지)
 * - 사전 승인 템플릿만 사용
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoAlimtalkService {

    private final NotificationLogRepository notificationLogRepository;

    @Value("${kakao.alimtalk.api-url:https://kapi.kakao.com}")
    private String kakaoApiUrl;

    @Value("${kakao.alimtalk.sender-key:}")
    private String senderKey;

    private static final LocalTime SEND_START = LocalTime.of(8, 0);
    private static final LocalTime SEND_END = LocalTime.of(21, 0);

    public NotificationLog send(SendNotificationRequest request) {
        // 야간 발송 차단
        LocalTime now = LocalTime.now();
        if (now.isBefore(SEND_START) || now.isAfter(SEND_END)) {
            log.warn("Night-time send blocked: user={}, type={}", request.getUserId(), request.getType());
            NotificationLog logEntry = createLog(request);
            logEntry.markFailed();
            return notificationLogRepository.save(logEntry);
        }

        NotificationLog logEntry = createLog(request);

        try {
            // TODO: 실제 카카오 알림톡 API 호출
            // 현재는 로그만 남기고 성공 처리 (API Key 발급 후 연동)
            log.info("Sending alimtalk: user={}, type={}, message={}",
                    request.getUserId(), request.getType(),
                    request.getMessage().substring(0, Math.min(50, request.getMessage().length())));

            logEntry.markSent();
        } catch (Exception e) {
            log.error("Alimtalk send failed: {}", e.getMessage());
            logEntry.markFailed();
        }

        return notificationLogRepository.save(logEntry);
    }

    private NotificationLog createLog(SendNotificationRequest request) {
        return NotificationLog.builder()
                .userId(request.getUserId())
                .type(request.getType())
                .message(request.getMessage())
                .build();
    }
}
