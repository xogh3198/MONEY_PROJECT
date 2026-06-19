package com.dividendbot.engine.dto;

public record NotificationPreferenceUpdateDto(
        boolean enabled,
        boolean alertTimingD7,
        boolean alertTimingD3,
        boolean alertTimingD1
) {
}
