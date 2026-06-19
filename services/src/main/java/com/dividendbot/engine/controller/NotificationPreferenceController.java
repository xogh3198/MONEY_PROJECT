package com.dividendbot.engine.controller;

import com.dividendbot.engine.dto.NotificationPreferenceDto;
import com.dividendbot.engine.dto.NotificationPreferenceUpdateDto;
import com.dividendbot.engine.service.NotificationPreferenceService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notification-preferences")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationPreferenceController {

    private final NotificationPreferenceService notificationPreferenceService;

    @GetMapping
    public ResponseEntity<NotificationPreferenceDto> getPreference(HttpServletRequest request) {
        UUID userId = (UUID) request.getAttribute("userId");
        return ResponseEntity.ok(notificationPreferenceService.getPreference(userId));
    }

    @PutMapping
    public ResponseEntity<NotificationPreferenceDto> updatePreference(HttpServletRequest request,
                                                                      @RequestBody NotificationPreferenceUpdateDto dto) {
        UUID userId = (UUID) request.getAttribute("userId");
        return ResponseEntity.ok(notificationPreferenceService.updatePreference(userId, dto));
    }
}
