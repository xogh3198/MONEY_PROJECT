package com.dividendbot.engine.service;

import com.dividendbot.engine.domain.entity.NotificationPreference;
import com.dividendbot.engine.domain.repository.NotificationPreferenceRepository;
import com.dividendbot.engine.dto.NotificationPreferenceDto;
import com.dividendbot.engine.dto.NotificationPreferenceUpdateDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository notificationPreferenceRepository;

    /**
     * 사용자의 알림 설정을 조회합니다.
     * 미존재 시 기본값으로 생성 후 반환합니다.
     */
    @Transactional
    public NotificationPreferenceDto getPreference(UUID userId) {
        NotificationPreference pref = notificationPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreference(userId));
        return toDto(pref);
    }

    /**
     * 사용자의 알림 설정을 업데이트합니다.
     * 미존재 시 기본값으로 먼저 생성 후 업데이트합니다.
     */
    @Transactional
    public NotificationPreferenceDto updatePreference(UUID userId, NotificationPreferenceUpdateDto dto) {
        NotificationPreference pref = notificationPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreference(userId));

        pref.updatePreferences(
                dto.enabled(),
                dto.alertTimingD7(),
                dto.alertTimingD3(),
                dto.alertTimingD1()
        );

        NotificationPreference saved = notificationPreferenceRepository.save(pref);
        log.info("Notification preference updated: userId={}, enabled={}", userId, dto.enabled());
        return toDto(saved);
    }

    /**
     * 사용자에 대한 기본 알림 설정을 생성합니다.
     * 기본값: enabled=true, D7=false, D3=true, D1=true
     */
    @Transactional
    public NotificationPreference createDefaultPreference(UUID userId) {
        NotificationPreference pref = NotificationPreference.builder()
                .userId(userId)
                .enabled(true)
                .alertTimingD7(false)
                .alertTimingD3(true)
                .alertTimingD1(true)
                .build();

        NotificationPreference saved = notificationPreferenceRepository.save(pref);
        log.info("Default notification preference created: userId={}", userId);
        return saved;
    }

    private NotificationPreferenceDto toDto(NotificationPreference pref) {
        return new NotificationPreferenceDto(
                pref.isEnabled(),
                pref.isAlertTimingD7(),
                pref.isAlertTimingD3(),
                pref.isAlertTimingD1()
        );
    }
}
