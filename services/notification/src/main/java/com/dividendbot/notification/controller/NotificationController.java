package com.dividendbot.notification.controller;

import com.dividendbot.notification.domain.entity.NotificationLog;
import com.dividendbot.notification.dto.SendNotificationRequest;
import com.dividendbot.notification.service.KakaoAlimtalkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final KakaoAlimtalkService kakaoAlimtalkService;

    @PostMapping("/send")
    public ResponseEntity<NotificationLog> sendNotification(
            @Valid @RequestBody SendNotificationRequest request) {
        NotificationLog log = kakaoAlimtalkService.send(request);
        return ResponseEntity.ok(log);
    }
}
