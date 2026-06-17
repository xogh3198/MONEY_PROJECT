package com.dividendbot.engine.scheduler;

import com.dividendbot.engine.domain.entity.DividendInfo;
import com.dividendbot.engine.domain.entity.Portfolio;
import com.dividendbot.engine.domain.repository.DividendInfoRepository;
import com.dividendbot.engine.domain.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * 배당락일 알림 자동 트리거
 * - 매일 08:00 KST 실행
 * - D-3, D-1 배당락일 보유자에게 알림 발송 요청
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExDividendAlertScheduler {

    private final DividendInfoRepository dividendInfoRepository;
    private final PortfolioRepository portfolioRepository;

    @Value("${services.notification.url:http://localhost:8082}")
    private String notificationServiceUrl;

    /**
     * 매일 08:00 KST — 배당락일 알림 체크 & 발송
     */
    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Seoul")
    public void checkAndSendExDividendAlerts() {
        LocalDate today = LocalDate.now();
        log.info("=== 배당락일 알림 체크 시작: {} ===", today);

        // D-3, D-1 배당락일 종목 조회
        LocalDate d3 = today.plusDays(3);
        LocalDate d1 = today.plusDays(1);

        List<DividendInfo> d3Stocks = dividendInfoRepository.findByExDividendDateBetween(d3, d3);
        List<DividendInfo> d1Stocks = dividendInfoRepository.findByExDividendDateBetween(d1, d1);

        sendAlerts(d3Stocks, "EX_DATE_D3", 3);
        sendAlerts(d1Stocks, "EX_DATE_D1", 1);

        log.info("=== 배당락일 알림 체크 완료: D-3 {}건, D-1 {}건 ===",
                d3Stocks.size(), d1Stocks.size());
    }

    private void sendAlerts(List<DividendInfo> dividends, String alertType, int daysUntil) {
        WebClient client = WebClient.create(notificationServiceUrl);

        for (DividendInfo div : dividends) {
            // 이 종목을 보유한 사용자 목록
            List<Portfolio> holders = portfolioRepository.findAll().stream()
                    .filter(p -> p.getStockCode().equals(div.getStockCode()))
                    .toList();

            for (Portfolio portfolio : holders) {
                BigDecimal expectedDividend = div.getDividendPerShare()
                        .multiply(BigDecimal.valueOf(portfolio.getQuantity()))
                        .setScale(0, RoundingMode.FLOOR);

                String message = String.format(
                        "배당락일 알림 (D-%d)\n━━━━━━━━━━━━━━━\n종목: %s\n배당락일: %s\n보유수량: %d주\n예상 배당금: %s원 (세전)\n\n※ 본 정보는 투자 조언이 아닙니다.",
                        daysUntil, div.getStockCode(), div.getExDividendDate(),
                        portfolio.getQuantity(), String.format("%,d", expectedDividend.longValue()));

                try {
                    client.post()
                            .uri("/api/notifications/send")
                            .bodyValue(Map.of(
                                    "userId", portfolio.getUserId().toString(),
                                    "type", alertType,
                                    "message", message))
                            .retrieve()
                            .bodyToMono(Void.class)
                            .block();

                    log.debug("Alert sent: user={}, stock={}, type={}",
                            portfolio.getUserId(), div.getStockCode(), alertType);
                } catch (Exception e) {
                    log.error("Alert send failed: user={}, stock={}: {}",
                            portfolio.getUserId(), div.getStockCode(), e.getMessage());
                }
            }
        }
    }
}
