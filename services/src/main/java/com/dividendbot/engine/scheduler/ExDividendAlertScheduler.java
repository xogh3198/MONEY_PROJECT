package com.dividendbot.engine.scheduler;

import com.dividendbot.engine.domain.entity.DividendInfo;
import com.dividendbot.engine.domain.entity.NotificationPreference;
import com.dividendbot.engine.domain.entity.Portfolio;
import com.dividendbot.engine.domain.repository.DividendInfoRepository;
import com.dividendbot.engine.domain.repository.NotificationPreferenceRepository;
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
import java.util.*;

/**
 * 배당락일 알림 자동 트리거
 * - 매일 08:00 KST 실행
 * - D-7, D-3, D-1 배당락일 보유자에게 알림 발송 요청
 * - 사용자별 NotificationPreference 반영
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExDividendAlertScheduler {

    private final DividendInfoRepository dividendInfoRepository;
    private final PortfolioRepository portfolioRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;

    @Value("${services.notification.url:http://localhost:8082}")
    private String notificationServiceUrl;

    /**
     * 매일 08:00 KST — 배당락일 알림 체크 & 발송
     */
    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Seoul")
    public void checkAndSendExDividendAlerts() {
        LocalDate today = LocalDate.now();
        log.info("=== 배당락일 알림 체크 시작: {} ===", today);

        // D-7, D-3, D-1 배당락일 종목 조회 (DividendInfo 테이블 기반)
        LocalDate d7 = today.plusDays(7);
        LocalDate d3 = today.plusDays(3);
        LocalDate d1 = today.plusDays(1);

        List<DividendInfo> d7Stocks = dividendInfoRepository.findByExDividendDateBetween(d7, d7);
        List<DividendInfo> d3Stocks = dividendInfoRepository.findByExDividendDateBetween(d3, d3);
        List<DividendInfo> d1Stocks = dividendInfoRepository.findByExDividendDateBetween(d1, d1);

        sendAlerts(d7Stocks, "EX_DATE_D7", 7);
        sendAlerts(d3Stocks, "EX_DATE_D3", 3);
        sendAlerts(d1Stocks, "EX_DATE_D1", 1);

        // Portfolio의 exDividendDate 필드 기반 알림 (사용자 수동 입력 배당락일)
        checkPortfolioExDates(today);

        log.info("=== 배당락일 알림 체크 완료: D-7 {}건, D-3 {}건, D-1 {}건 ===",
                d7Stocks.size(), d3Stocks.size(), d1Stocks.size());
    }

    private void sendAlerts(List<DividendInfo> dividends, String alertType, int daysUntil) {
        WebClient client = WebClient.create(notificationServiceUrl);

        for (DividendInfo div : dividends) {
            // 이 종목을 보유한 사용자 목록
            List<Portfolio> holders = portfolioRepository.findAll().stream()
                    .filter(p -> p.getStockCode().equals(div.getStockCode()))
                    .toList();

            for (Portfolio portfolio : holders) {
                try {
                    // 사용자 알림 preference 확인
                    if (!shouldSendAlert(portfolio.getUserId(), daysUntil)) {
                        continue;
                    }

                    BigDecimal expectedDividend = div.getDividendPerShare()
                            .multiply(BigDecimal.valueOf(portfolio.getQuantity()))
                            .setScale(0, RoundingMode.FLOOR);

                    String message = String.format(
                            "배당락일 알림 (D-%d)\n━━━━━━━━━━━━━━━\n종목: %s\n배당락일: %s\n보유수량: %d주\n예상 배당금: %s원 (세전)\n\n※ 본 정보는 투자 조언이 아닙니다.",
                            daysUntil, div.getStockCode(), div.getExDividendDate(),
                            portfolio.getQuantity(), String.format("%,d", expectedDividend.longValue()));

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

    /**
     * Portfolio 엔티티의 exDividendDate 필드 기반으로 알림 체크.
     * DividendInfo 테이블에 없더라도 사용자가 수동 입력한 배당락일에 대해 알림 발송.
     */
    private void checkPortfolioExDates(LocalDate today) {
        LocalDate d7 = today.plusDays(7);
        LocalDate d3 = today.plusDays(3);
        LocalDate d1 = today.plusDays(1);

        WebClient client = WebClient.create(notificationServiceUrl);

        // D-7 매칭 포트폴리오
        sendPortfolioAlerts(client, portfolioRepository.findByExDividendDate(d7), "EX_DATE_D7", 7);
        // D-3 매칭 포트폴리오
        sendPortfolioAlerts(client, portfolioRepository.findByExDividendDate(d3), "EX_DATE_D3", 3);
        // D-1 매칭 포트폴리오
        sendPortfolioAlerts(client, portfolioRepository.findByExDividendDate(d1), "EX_DATE_D1", 1);
    }

    private void sendPortfolioAlerts(WebClient client, List<Portfolio> portfolios, String alertType, int daysUntil) {
        for (Portfolio portfolio : portfolios) {
            try {
                // 사용자 알림 preference 확인
                if (!shouldSendAlert(portfolio.getUserId(), daysUntil)) {
                    continue;
                }

                BigDecimal dividendPerShare = portfolio.getDividendPerShare() != null
                        ? portfolio.getDividendPerShare() : BigDecimal.ZERO;
                BigDecimal expectedDividend = dividendPerShare
                        .multiply(BigDecimal.valueOf(portfolio.getQuantity()))
                        .setScale(0, RoundingMode.FLOOR);

                String message = String.format(
                        "배당락일 알림 (D-%d)\n━━━━━━━━━━━━━━━\n종목: %s (%s)\n배당락일: %s\n보유수량: %d주\n예상 배당금: %s원 (세전)\n\n※ 본 정보는 투자 조언이 아닙니다.",
                        daysUntil, portfolio.getStockName(), portfolio.getStockCode(),
                        portfolio.getExDividendDate(),
                        portfolio.getQuantity(), String.format("%,d", expectedDividend.longValue()));

                client.post()
                        .uri("/api/notifications/send")
                        .bodyValue(Map.of(
                                "userId", portfolio.getUserId().toString(),
                                "type", alertType,
                                "message", message))
                        .retrieve()
                        .bodyToMono(Void.class)
                        .block();

                log.debug("Portfolio alert sent: user={}, stock={}, type={}",
                        portfolio.getUserId(), portfolio.getStockCode(), alertType);
            } catch (Exception e) {
                log.error("Portfolio alert send failed: user={}, stock={}: {}",
                        portfolio.getUserId(), portfolio.getStockCode(), e.getMessage());
            }
        }
    }

    /**
     * 사용자의 NotificationPreference를 확인하여 알림을 보낼지 결정.
     * - preference가 없으면 기본값 적용 (D-3, D-1 활성화, D-7 비활성화)
     * - preference.enabled == false → 알림 안 보냄
     * - 해당 타이밍 flag가 false → 알림 안 보냄
     */
    private boolean shouldSendAlert(UUID userId, int daysUntil) {
        Optional<NotificationPreference> prefOpt = notificationPreferenceRepository.findByUserId(userId);

        if (prefOpt.isPresent()) {
            NotificationPreference pref = prefOpt.get();
            if (!pref.isEnabled()) return false;
            if (daysUntil == 7 && !pref.isAlertTimingD7()) return false;
            if (daysUntil == 3 && !pref.isAlertTimingD3()) return false;
            if (daysUntil == 1 && !pref.isAlertTimingD1()) return false;
            return true;
        } else {
            // No preference record = use defaults (D-3, D-1 enabled, D-7 disabled)
            if (daysUntil == 7) return false; // D-7 disabled by default
            return true; // D-3, D-1 enabled by default
        }
    }
}
