package com.dividendbot.news.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "growth_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GrowthEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_name", nullable = false, length = 64)
    private String eventName;

    @Column(name = "visitor_id_hash", nullable = false, length = 64)
    private String visitorIdHash;

    @Column(name = "session_id_hash", nullable = false, length = 64)
    private String sessionIdHash;

    @Column(nullable = false, length = 255)
    private String path;

    @Column(name = "utm_source", length = 120)
    private String utmSource;

    @Column(name = "utm_medium", length = 120)
    private String utmMedium;

    @Column(name = "utm_campaign", length = 120)
    private String utmCampaign;

    @Column(name = "utm_content", length = 120)
    private String utmContent;

    @Column(name = "properties_json", nullable = false, columnDefinition = "TEXT")
    private String propertiesJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static GrowthEvent create(
            String eventName,
            String visitorIdHash,
            String sessionIdHash,
            String path,
            String utmSource,
            String utmMedium,
            String utmCampaign,
            String utmContent,
            String propertiesJson
    ) {
        GrowthEvent event = new GrowthEvent();
        event.eventName = trim(eventName, 64);
        event.visitorIdHash = trim(visitorIdHash, 64);
        event.sessionIdHash = trim(sessionIdHash, 64);
        event.path = trim(path, 255);
        event.utmSource = nullableTrim(utmSource, 120);
        event.utmMedium = nullableTrim(utmMedium, 120);
        event.utmCampaign = nullableTrim(utmCampaign, 120);
        event.utmContent = nullableTrim(utmContent, 120);
        event.propertiesJson = propertiesJson == null || propertiesJson.isBlank() ? "{}" : propertiesJson;
        event.createdAt = LocalDateTime.now();
        return event;
    }

    private static String trim(String value, int limit) {
        String normalized = value == null ? "" : value.replaceAll("[\\r\\n\\t]+", " ").trim();
        return normalized.length() <= limit ? normalized : normalized.substring(0, limit);
    }

    private static String nullableTrim(String value, int limit) {
        String normalized = trim(value, limit);
        return normalized.isBlank() ? null : normalized;
    }
}
