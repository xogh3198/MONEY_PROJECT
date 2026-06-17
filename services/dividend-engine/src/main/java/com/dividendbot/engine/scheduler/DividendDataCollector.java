package com.dividendbot.engine.scheduler;

import com.dividendbot.engine.domain.entity.DividendInfo;
import com.dividendbot.engine.domain.repository.DividendInfoRepository;
import com.dividendbot.engine.infra.kis.KisApiClient;
import com.dividendbot.engine.infra.kis.dto.KisDividendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 배당 데이터 일일 수집 스케줄러
 * - 매일 02:00 KST 실행
 * - 한투 API로 배당 종목 정보 수집
 * - Rate Limit 준수 (초당 20건)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DividendDataCollector {

    private final KisApiClient kisApiClient;
    private final DividendInfoRepository dividendInfoRepository;

    // 수집 대상 종목 (MVP: 주요 배당주 고정 목록, Phase 4에서 동적 확장)
    private static final List<String> TARGET_STOCKS = List.of(
            "005930",  // 삼성전자
            "000660",  // SK하이닉스
            "035720",  // 카카오
            "005380",  // 현대차
            "051910",  // LG화학
            "006400",  // 삼성SDI
            "003550",  // LG
            "017670",  // SK텔레콤
            "034730",  // SK
            "032830"   // 삼성생명
    );

    /**
     * 매일 02:00 KST 배당 데이터 수집
     */
    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
    public void collectDailyDividendData() {
        log.info("=== 배당 데이터 수집 시작 ({}개 종목) ===", TARGET_STOCKS.size());
        int success = 0;
        int failed = 0;

        for (String stockCode : TARGET_STOCKS) {
            try {
                KisDividendResponse response = kisApiClient.getDividendInfo(stockCode);
                if (response.isSuccess()) {
                    // TODO: 실제 배당금 정보는 별도 API 호출 필요
                    // 현재는 현재가 정보만 수집, 배당 정보는 시드 데이터 사용
                    log.debug("Collected: {} ({}), price={}",
                            response.getStockName(), stockCode, response.getCurrentPrice());
                    success++;
                } else {
                    failed++;
                }

                // Rate Limit: 초당 20건 → 50ms 간격
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Failed to collect {}: {}", stockCode, e.getMessage());
                failed++;
            }
        }

        log.info("=== 배당 데이터 수집 완료: 성공 {}, 실패 {} ===", success, failed);
    }

    /**
     * 수동 트리거 (테스트/관리자용)
     */
    public void triggerManualCollection() {
        collectDailyDividendData();
    }
}
