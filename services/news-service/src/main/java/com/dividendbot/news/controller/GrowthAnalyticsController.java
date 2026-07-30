package com.dividendbot.news.controller;

import com.dividendbot.news.dto.GrowthAnalyticsSummary;
import com.dividendbot.news.dto.GrowthEventRequest;
import com.dividendbot.news.service.GrowthAnalyticsAccessGuard;
import com.dividendbot.news.service.GrowthAnalyticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class GrowthAnalyticsController {

    private final GrowthAnalyticsService service;
    private final GrowthAnalyticsAccessGuard accessGuard;

    @PostMapping("/events")
    public ResponseEntity<Void> record(@Valid @RequestBody GrowthEventRequest request) {
        service.record(request);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/summary")
    public ResponseEntity<GrowthAnalyticsSummary> summary(
            @RequestHeader(value = "X-Video-Render-Key", required = false) String accessKey,
            @RequestParam(defaultValue = "30") int days
    ) {
        accessGuard.requireAuthorized(accessKey);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.summary(days));
    }
}
