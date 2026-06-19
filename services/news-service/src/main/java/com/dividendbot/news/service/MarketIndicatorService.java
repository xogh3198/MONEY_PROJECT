package com.dividendbot.news.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 시장 지표 실시간 수집 서비스
 * 소스: Yahoo Finance (무료, API Key 불필요)
 *
 * Yahoo Finance 티커:
 * - ^KS11 = 코스피
 * - ^KQ11 = 코스닥
 * - KRW=X = USD/KRW 환율
 * - ^GSPC = S&P 500
 * - BTC-USD = 비트코인
 * - GC=F = 금
 */
@Service
@Slf4j
public class MarketIndicatorService {

    private final WebClient yahooClient;
    private final Map<String, IndicatorData> cache = new ConcurrentHashMap<>();

    private static final Map<String, TickerInfo> TICKERS = Map.of(
            "KOSPI", new TickerInfo("^KS11", "코스피"),
            "KOSDAQ", new TickerInfo("^KQ11", "코스닥"),
            "USD_KRW", new TickerInfo("KRW=X", "원/달러"),
            "SP500", new TickerInfo("^GSPC", "S&P 500"),
            "BTC", new TickerInfo("BTC-USD", "비트코인"),
            "GOLD", new TickerInfo("GC=F", "금")
    );

    public MarketIndicatorService() {
        this.yahooClient = WebClient.builder()
                .baseUrl("https://query1.finance.yahoo.com")
                .defaultHeader("User-Agent", "Mozilla/5.0")
                .build();

        // 초기 로딩
        refreshAllIndicators();
    }

    /**
     * 5분 간격으로 Yahoo Finance에서 실데이터 갱신
     */
    @Scheduled(fixedRate = 60000) // 1분
    public void refreshAllIndicators() {
        log.info("=== 시장 지표 갱신 시작 (Yahoo Finance) ===");
        int success = 0;

        for (Map.Entry<String, TickerInfo> entry : TICKERS.entrySet()) {
            String key = entry.getKey();
            TickerInfo ticker = entry.getValue();

            try {
                Map<String, Object> data = fetchFromYahoo(ticker.symbol);
                if (data != null) {
                    double price = toDouble(data.get("regularMarketPrice"));
                    double prevClose = toDouble(data.get("chartPreviousClose"));
                    if (prevClose == 0) prevClose = toDouble(data.get("previousClose"));
                    if (prevClose == 0) prevClose = toDouble(data.get("regularMarketPreviousClose"));
                    double change = price - prevClose;
                    double changePercent = prevClose > 0 ? (change / prevClose) * 100 : 0;

                    cache.put(key, new IndicatorData(
                            ticker.name, price,
                            Math.round(changePercent * 100.0) / 100.0,
                            changePercent > 0.5 ? "UP" : changePercent < -0.5 ? "DOWN" : "NEUTRAL",
                            LocalDateTime.now()
                    ));
                    success++;
                }
            } catch (Exception e) {
                log.warn("Yahoo Finance 조회 실패 [{}]: {}", ticker.symbol, e.getMessage());
            }
        }

        log.info("=== 시장 지표 갱신 완료: {}/{} ===", success, TICKERS.size());
    }

    /**
     * Yahoo Finance v8 API 호출
     */
    private Map<String, Object> fetchFromYahoo(String symbol) {
        try {
            Map response = yahooClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v8/finance/chart/" + symbol)
                            .queryParam("interval", "1d")
                            .queryParam("range", "2d")
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response == null) return null;

            Map chart = (Map) response.get("chart");
            if (chart == null) return null;

            List<Map> results = (List<Map>) chart.get("result");
            if (results == null || results.isEmpty()) return null;

            Map meta = (Map) results.get(0).get("meta");
            return meta;
        } catch (Exception e) {
            log.debug("Yahoo API error for {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    public List<Map<String, Object>> getAllIndicators() {
        // 캐시가 비어있으면 기본값 반환
        if (cache.isEmpty()) {
            return getDefaultIndicators();
        }

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

    public List<Map<String, Object>> getHistory(String type, int days, String interval) {
        // Yahoo Finance 히스토리 조회
        TickerInfo ticker = TICKERS.get(type);
        if (ticker == null) return List.of();

        // interval 유효성 검사 및 range 결정
        String validInterval = switch (interval) {
            case "1m" -> "1m";
            case "5m" -> "5m";
            case "15m" -> "15m";
            case "1h" -> "1h";
            default -> "1d";
        };

        // interval에 따른 적절한 range 설정
        String range = switch (validInterval) {
            case "1m" -> "1d";       // 1분봉: 최근 1일
            case "5m" -> "5d";       // 5분봉: 최근 5일
            case "15m" -> "5d";      // 15분봉: 최근 5일
            case "1h" -> days <= 7 ? "5d" : "1mo";  // 1시간봉: 5일~1개월
            default -> days <= 5 ? "5d" : days <= 30 ? "1mo" : days <= 90 ? "3mo" : "1y";
        };

            Map response = yahooClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v8/finance/chart/" + ticker.symbol)
                            .queryParam("interval", validInterval)
                            .queryParam("range", range)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response == null) return List.of();

            Map chart = (Map) response.get("chart");
            List<Map> results = (List<Map>) chart.get("result");
            if (results == null || results.isEmpty()) return List.of();

            Map result = results.get(0);
            List<Integer> timestamps = (List<Integer>) result.get("timestamp");
            Map indicators = (Map) result.get("indicators");
            List<Map> quotes = (List<Map>) indicators.get("quote");
            List<Number> closes = (List<Number>) quotes.get(0).get("close");

            List<Map<String, Object>> history = new ArrayList<>();
            for (int i = 0; i < Math.min(timestamps.size(), closes.size()); i++) {
                if (closes.get(i) != null) {
                    String dateStr;
                    if ("1d".equals(validInterval)) {
                        dateStr = java.time.Instant.ofEpochSecond(timestamps.get(i)).toString().substring(0, 10);
                    } else {
                        // For intraday, include time
                        dateStr = java.time.Instant.ofEpochSecond(timestamps.get(i))
                                .atZone(java.time.ZoneId.of("Asia/Seoul"))
                                .toLocalDateTime().toString().substring(0, 16);
                    }
                    history.add(Map.of(
                            "date", dateStr,
                            "value", closes.get(i).doubleValue()
                    ));
                }
            }
            return history;
        } catch (Exception e) {
            log.warn("Yahoo history fetch failed for {}: {}", type, e.getMessage());
            return List.of();
        }
    }

    private List<Map<String, Object>> getDefaultIndicators() {
        return List.of(
                Map.of("type", "KOSPI", "name", "코스피", "value", 2847.52, "changePercent", 0.0, "prediction", "NEUTRAL", "updatedAt", "loading"),
                Map.of("type", "KOSDAQ", "name", "코스닥", "value", 892.15, "changePercent", 0.0, "prediction", "NEUTRAL", "updatedAt", "loading"),
                Map.of("type", "USD_KRW", "name", "원/달러", "value", 1342.5, "changePercent", 0.0, "prediction", "NEUTRAL", "updatedAt", "loading"),
                Map.of("type", "SP500", "name", "S&P 500", "value", 5892.3, "changePercent", 0.0, "prediction", "NEUTRAL", "updatedAt", "loading"),
                Map.of("type", "BTC", "name", "비트코인", "value", 98452.0, "changePercent", 0.0, "prediction", "NEUTRAL", "updatedAt", "loading")
        );
    }

    private double toDouble(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try { return Double.parseDouble(value.toString()); } catch (Exception e) { return 0; }
    }

    private record TickerInfo(String symbol, String name) {}

    private record IndicatorData(String name, double value, double changePercent, String prediction, LocalDateTime updatedAt) {}
}
