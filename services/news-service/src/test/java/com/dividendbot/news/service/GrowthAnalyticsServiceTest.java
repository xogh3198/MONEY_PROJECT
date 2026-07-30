package com.dividendbot.news.service;

import com.dividendbot.news.domain.entity.GrowthEvent;
import com.dividendbot.news.domain.repository.GrowthEventRepository;
import com.dividendbot.news.dto.GrowthAnalyticsSummary;
import com.dividendbot.news.dto.GrowthEventRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GrowthAnalyticsServiceTest {

    private static final String VISITOR_ID = "visitor-1234567890";
    private static final String SESSION_ID = "session-1234567890";

    @Test
    void storesOnlyHashedIdentifiersAndSanitizedProperties() {
        GrowthEventRepository repository = mock(GrowthEventRepository.class);
        when(repository.countByVisitorIdHashAndCreatedAtAfter(any(), any())).thenReturn(0L);
        GrowthAnalyticsService service = new GrowthAnalyticsService(repository, new ObjectMapper());

        service.record(request("promotion_plan_created", Map.of(
                "goal", "상담 문의\n다음 줄",
                "budget", 300_000
        )));

        ArgumentCaptor<GrowthEvent> captor = ArgumentCaptor.forClass(GrowthEvent.class);
        verify(repository).save(captor.capture());
        GrowthEvent saved = captor.getValue();
        assertThat(saved.getVisitorIdHash()).hasSize(64).doesNotContain(VISITOR_ID);
        assertThat(saved.getSessionIdHash()).hasSize(64).doesNotContain(SESSION_ID);
        assertThat(saved.getPropertiesJson()).contains("\"budget\":300000", "상담 문의 다음 줄");
    }

    @Test
    void rejectsUnknownEvents() {
        GrowthEventRepository repository = mock(GrowthEventRepository.class);
        GrowthAnalyticsService service = new GrowthAnalyticsService(repository, new ObjectMapper());

        assertThatThrownBy(() -> service.record(request("unknown_event", Map.of())))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
        verify(repository, never()).save(any());
    }

    @Test
    void rateLimitDropsExcessEvents() {
        GrowthEventRepository repository = mock(GrowthEventRepository.class);
        when(repository.countByVisitorIdHashAndCreatedAtAfter(any(), any())).thenReturn(60L);
        GrowthAnalyticsService service = new GrowthAnalyticsService(repository, new ObjectMapper());

        service.record(request("page_view", Map.of()));

        verify(repository, never()).save(any());
    }

    @Test
    void summarizesUniqueAndQualifiedVisitorsWithinBoundedWindow() {
        GrowthEventRepository repository = mock(GrowthEventRepository.class);
        GrowthEvent first = GrowthEvent.create(
                "page_view", "visitor-a", "session-a", "/", null, null, "launch", null, "{}"
        );
        GrowthEvent second = GrowthEvent.create(
                "promotion_plan_created", "visitor-a", "session-a", "/promotion-map",
                "naver", "organic", "launch", null, "{}"
        );
        GrowthEvent third = GrowthEvent.create(
                "guide_next_action", "visitor-b", "session-b", "/guides/compound-interest-time",
                null, null, null, null, "{}"
        );
        when(repository.findByCreatedAtAfterOrderByCreatedAtDesc(
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(first, second, third)));
        GrowthAnalyticsService service = new GrowthAnalyticsService(repository, new ObjectMapper());

        GrowthAnalyticsSummary summary = service.summary(365);

        assertThat(summary.days()).isEqualTo(90);
        assertThat(summary.totalEvents()).isEqualTo(3);
        assertThat(summary.uniqueVisitors()).isEqualTo(2);
        assertThat(summary.qualifiedVisitors()).isEqualTo(2);
        assertThat(summary.events()).containsEntry("page_view", 1L)
                .containsEntry("promotion_plan_created", 1L);
        assertThat(summary.campaigns()).containsEntry("launch", 2L);
    }

    private GrowthEventRequest request(String eventName, Map<String, Object> properties) {
        return new GrowthEventRequest(
                eventName,
                VISITOR_ID,
                SESSION_ID,
                "/promotion-map",
                "internal",
                "site",
                "launch",
                null,
                properties
        );
    }
}
