package com.dividendbot.news.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 시장 지표 수집 & 제공 서비스
 * - 1분 간격 갱신 (장중)
 * - 캐시된 지표를 API로 제공
 * - TODO Phase 9: Redis 캐시 전환, 실제 시세 API 연동
 */
@Service
@Slf4j
public class MarketIndicatorService {

    private final Map<String, IndicatorData> cache = new ConcurrentHashMap<>();
    private final Map<String, List<Map<String, Object>>> historyCache = new ConcurrentHashMap<>();
    private final WebClient webClient = WebClient.create();

    public MarketIndicatorService() {
        // 초기 데이터 세팅
        initializeIndicators();
    }

    /**
     * 1분 간격 시장 지표 갱신
     */
    @Scheduled(fixedRate = 60000)
    public void refreshIndicators() {
        // TODO: 실제 시세 API 연동 (한투 API, Yahoo Finance 등)
        // 현재: 시뮬레이션 데이터 (실감나는 변동)
        for (Map.Entry<String, IndicatorData> entry : cache.entrySet()) {
            IndicatorData data = entry.getValue();
            double change = (Math.random() - 0.48) * data.volatility;
            double newValue = data.value * (1 + change / 100);
            data.value = Math.round(newValue * 100.0) / 100.0;
            data.changePercent = BigDecimal.valueOf(change).setScale(2, RoundingMode.HALF_UP).doubleValue();
            data.updatedAt = LocalDateTime.now();

            // 히스토리 추가
            historyCache.computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
                    .add(Map.of("date", LocalDateTime.now().toString(), "value", data.value));
        }
    }

    public List<Map<String, Object>> getAllIndicators() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, IndicatorData> entry : cache.entrySet()) {
            IndicatorData d = entry.getValue();
            result.add(Map.of(
                    "type", entry.getKey(),
                    "name", d.name,
                    "value", d.value,
                    "changePercent", d.changePercent,
                    "prediction", d.prediction,
                    "updatedAt", d.updatedAt.toString()
            ));
        }
        return result;
    }

    public List<Map<String, Object>> getHistory(String type, int days) {
        List<Map<String, Object>> history = historyCache.getOrDefault(type, List.of());
        // 최근 N일치만 반환 (1분 간격이므로 days * 60 * 24 ≈ 너무 많음, 일봉 기준 제공)
        int limit = Math.min(days, history.size());
        return history.subList(Math.max(0, history.size() - limit), history.size());
    }

    private void initializeIndicators() {
        cache.put("KOSPI", new IndicatorData("코스피", 2847.52, 0.8, "UP"));
        cache.put("KOSDAQ", new IndicatorData("코스닥", 892.15, 1.2, "NEUTRAL"));
        cache.put("USD_KRW", new IndicatorData("원/달러", 1342.50, 0.3, "UP"));
        cache.put("SP500", new IndicatorData("S&P 500", 5892.30, 0.5, "UP"));
        cache.put("BTC", new IndicatorData("비트코인", 98452, 2.5, "NEUTRAL"));
        cache.put("GOLD", new IndicatorData("금", 2380, 0.4, "UP"));

        // 초기 30일 히스토리 생성
        for (Map.Entry<String, IndicatorData> entry : cache.entrySet()) {
            List<Map<String, Object>> history = new ArrayList<>();
            double v = entry.getValue().value;
            for (int i = 30; i >= 0; i--) {
                v += (Math.random() - 0.48) * entry.getValue().volatility * v / 100;
                history.add(Map.of(
                        "date", LocalDate.now().minusDays(i).toString(),
                        "value", Math.round(v * 100.0) / 100.0));
            }
            historyCache.put(entry.getKey(), history);
        }
    }

    private static class IndicatorData {
        String name;
        double value;
        double changePercent;
        double volatility;
        String prediction;
        LocalDateTime updatedAt;

        IndicatorData(String name, double value, double volatility, String prediction) {
            this.name = name;
            this.value = value;
            this.volatility = volatility;
            this.prediction = prediction;
            this.changePercent = 0;
            this.updatedAt = LocalDateTime.now();
        }
    }
}
