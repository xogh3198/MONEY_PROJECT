package com.dividendbot.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Dividend Engine 서비스 호출 클라이언트
 * 실제 REST API 호출 (타임아웃 3초 — 카카오 5초 제한 여유 확보)
 */
@Service
@Slf4j
public class DividendEngineClient {

    private final WebClient webClient;

    public DividendEngineClient(
            @Value("${services.dividend-engine.url:http://localhost:8080}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public String getMonthlyDividend(String kakaoUserId) {
        try {
            LocalDate now = LocalDate.now();
            Map response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/dividends/monthly")
                            .queryParam("kakao_user_id", kakaoUserId)
                            .queryParam("year", now.getYear())
                            .queryParam("month", now.getMonthValue())
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(3))
                    .block();

            if (response == null || response.get("items") == null) {
                return "등록된 포트폴리오가 없습니다.\n\"포트폴리오 등록\"으로 종목을 추가해주세요.";
            }

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
            if (items.isEmpty()) {
                return "이번 달 예상 배당금이 없습니다.";
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%s 예상 배당금\n", response.get("month")));
            sb.append("━━━━━━━━━━━━━━━\n");
            for (Map<String, Object> item : items) {
                sb.append(String.format("%s: %s원 (세후 %s원)\n",
                        item.get("stockName"),
                        formatNumber(item.get("preTax")),
                        formatNumber(item.get("afterTax"))));
            }
            sb.append("━━━━━━━━━━━━━━━\n");
            sb.append(String.format("합계: %s원 (세후 %s원)\n\n",
                    formatNumber(response.get("totalPreTax")),
                    formatNumber(response.get("totalAfterTax"))));
            sb.append("※ 본 정보는 투자 조언이 아닙니다.");
            return sb.toString();

        } catch (Exception e) {
            log.error("Failed to get monthly dividend: {}", e.getMessage());
            return "배당금 조회 중 일시적 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
        }
    }

    public String getUpcomingExDates(String kakaoUserId) {
        try {
            List<Map<String, Object>> exDates = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/dividends/ex-dates")
                            .queryParam("days_ahead", 7)
                            .build())
                    .retrieve()
                    .bodyToMono(List.class)
                    .timeout(Duration.ofSeconds(3))
                    .block();

            if (exDates == null || exDates.isEmpty()) {
                return "7일 이내 배당락일 종목이 없습니다.";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("다가오는 배당락일\n━━━━━━━━━━━━━━━\n");
            for (Map<String, Object> d : exDates) {
                sb.append(String.format("%s (%s)\n  배당락일: %s\n  주당배당: %s원\n\n",
                        d.get("stockCode"), d.get("stockCode"),
                        d.get("exDividendDate"),
                        d.get("dividendPerShare")));
            }
            sb.append("※ 본 정보는 투자 조언이 아닙니다.");
            return sb.toString();

        } catch (Exception e) {
            log.error("Failed to get ex-dates: {}", e.getMessage());
            return "배당락일 조회 중 오류가 발생했습니다.";
        }
    }

    public String getPortfolioSummary(String kakaoUserId) {
        try {
            List<Map<String, Object>> portfolios = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/portfolios")
                            .queryParam("kakao_user_id", kakaoUserId)
                            .build())
                    .retrieve()
                    .bodyToMono(List.class)
                    .timeout(Duration.ofSeconds(3))
                    .block();

            if (portfolios == null || portfolios.isEmpty()) {
                return "등록된 포트폴리오가 없습니다.\n\"삼성전자 100주 추가\"와 같이 입력해주세요.";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("나의 포트폴리오\n━━━━━━━━━━━━━━━\n");
            for (Map<String, Object> p : portfolios) {
                sb.append(String.format("%s %s주\n", p.get("stockName"), p.get("quantity")));
            }
            sb.append(String.format("━━━━━━━━━━━━━━━\n총 %d종목 보유", portfolios.size()));
            return sb.toString();

        } catch (Exception e) {
            log.error("Failed to get portfolio: {}", e.getMessage());
            return "포트폴리오 조회 중 오류가 발생했습니다.";
        }
    }

    private String formatNumber(Object value) {
        if (value == null) return "0";
        try {
            long num = Long.parseLong(value.toString().split("\\.")[0]);
            return String.format("%,d", num);
        } catch (NumberFormatException e) {
            return value.toString();
        }
    }
}
