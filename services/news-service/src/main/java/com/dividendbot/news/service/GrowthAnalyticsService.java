package com.dividendbot.news.service;

import com.dividendbot.news.domain.entity.GrowthEvent;
import com.dividendbot.news.domain.repository.GrowthEventRepository;
import com.dividendbot.news.dto.GrowthAnalyticsSummary;
import com.dividendbot.news.dto.GrowthEventRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GrowthAnalyticsService {

    private static final Set<String> ALLOWED_EVENTS = Set.of(
            "page_view",
            "qualified_read_50",
            "content_share",
            "tool_complete",
            "tool_option",
            "briefing_next_action",
            "guide_next_action",
            "interest_topics_saved",
            "interest_topics_opened",
            "promotion_source_analyzed",
            "promotion_plan_created",
            "promotion_plan_to_studio",
            "promotion_pricing_interest",
            "promotion_video_draft_created",
            "promotion_video_render_requested",
            "forum_article_open",
            "product_navigation",
            "landing_cta"
    );

    private static final Set<String> QUALIFIED_EVENTS = Set.of(
            "qualified_read_50",
            "tool_complete",
            "briefing_next_action",
            "guide_next_action",
            "interest_topics_saved",
            "promotion_plan_created",
            "promotion_video_draft_created",
            "promotion_video_render_requested"
    );

    private final GrowthEventRepository repository;
    private final ObjectMapper objectMapper;

    public void record(GrowthEventRequest request) {
        if (!ALLOWED_EVENTS.contains(request.eventName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 운영 이벤트입니다.");
        }

        String visitorHash = hash(request.visitorId());
        if (repository.countByVisitorIdHashAndCreatedAtAfter(visitorHash, LocalDateTime.now().minusMinutes(1)) >= 60) {
            return;
        }

        repository.save(GrowthEvent.create(
                request.eventName(),
                visitorHash,
                hash(request.sessionId()),
                request.path(),
                request.utmSource(),
                request.utmMedium(),
                request.utmCampaign(),
                request.utmContent(),
                safeProperties(request.properties())
        ));
    }

    public GrowthAnalyticsSummary summary(int requestedDays) {
        int days = Math.max(1, Math.min(requestedDays, 90));
        List<GrowthEvent> events = repository.findByCreatedAtAfterOrderByCreatedAtDesc(
                LocalDateTime.now().minusDays(days),
                PageRequest.of(0, 10_000)
        ).getContent();

        Map<String, Long> eventCounts = counts(events.stream().map(GrowthEvent::getEventName).toList());
        Map<String, Long> campaignCounts = counts(events.stream()
                .map(GrowthEvent::getUtmCampaign)
                .filter(value -> value != null && !value.isBlank())
                .toList());
        Map<String, Long> pathCounts = counts(events.stream().map(GrowthEvent::getPath).toList());
        long uniqueVisitors = events.stream().map(GrowthEvent::getVisitorIdHash).distinct().count();
        Set<String> qualifiedVisitorIds = events.stream()
                .filter(event -> QUALIFIED_EVENTS.contains(event.getEventName()))
                .map(GrowthEvent::getVisitorIdHash)
                .collect(Collectors.toSet());

        return new GrowthAnalyticsSummary(
                days,
                events.size(),
                uniqueVisitors,
                qualifiedVisitorIds.size(),
                eventCounts,
                campaignCounts,
                pathCounts,
                LocalDateTime.now()
        );
    }

    @Scheduled(cron = "0 15 3 * * *", zone = "Asia/Seoul")
    @Transactional
    public void deleteExpiredEvents() {
        repository.deleteByCreatedAtBefore(LocalDateTime.now().minusDays(180));
    }

    private Map<String, Long> counts(List<String> values) {
        Map<String, Long> result = new LinkedHashMap<>();
        values.forEach(value -> result.merge(value, 1L, Long::sum));
        return result;
    }

    private String safeProperties(Map<String, Object> input) {
        if (input == null || input.isEmpty()) return "{}";
        Map<String, Object> safe = new LinkedHashMap<>();
        input.entrySet().stream().limit(20).forEach(entry -> {
            String key = sanitize(entry.getKey(), 64);
            Object value = entry.getValue();
            if (key.isBlank() || value == null) return;
            if (value instanceof Number || value instanceof Boolean) {
                safe.put(key, value);
            } else {
                safe.put(key, sanitize(String.valueOf(value), 240));
            }
        });
        try {
            return objectMapper.writeValueAsString(safe);
        } catch (JsonProcessingException error) {
            return "{}";
        }
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte item : digest) hex.append(String.format("%02x", item));
            return hex.toString();
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", error);
        }
    }

    private String sanitize(String value, int limit) {
        String normalized = value == null ? "" : value.replaceAll("[\\r\\n\\t]+", " ").trim();
        return normalized.length() <= limit ? normalized : normalized.substring(0, limit);
    }
}
