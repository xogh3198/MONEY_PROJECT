package com.dividendbot.news.controller;

import com.dividendbot.news.service.MarketIndicatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MarketIndicatorController {

    private final MarketIndicatorService marketIndicatorService;

    /**
     * 주요 시장 지표 전체 조회 (실시간)
     */
    @GetMapping("/indicators")
    public ResponseEntity<List<Map<String, Object>>> getIndicators() {
        return ResponseEntity.ok(marketIndicatorService.getAllIndicators());
    }

    /**
     * 특정 지표 히스토리 (차트용)
     */
    @GetMapping("/indicators/{type}/history")
    public ResponseEntity<List<Map<String, Object>>> getHistory(
            @PathVariable String type,
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "1d") String interval) {
        return ResponseEntity.ok(marketIndicatorService.getHistory(type, days, interval));
    }
}
