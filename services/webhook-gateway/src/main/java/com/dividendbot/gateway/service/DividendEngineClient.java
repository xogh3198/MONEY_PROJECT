package com.dividendbot.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Dividend Engine 서비스 호출 클라이언트
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
        // MVP: 하드코딩된 응답 (실제 연동은 user 매핑 후)
        // TODO: kakaoUserId → UUID 매핑 후 dividend-engine API 호출
        return "6월 예상 배당금 요약\n" +
                "━━━━━━━━━━━━━━━\n" +
                "삼성전자: 36,100원 (세후 30,540원)\n" +
                "SK하이닉스: 12,000원 (세후 10,152원)\n" +
                "━━━━━━━━━━━━━━━\n" +
                "합계: 48,100원 (세후 40,692원)\n\n" +
                "※ 본 정보는 투자 조언이 아닙니다.";
    }

    public String getUpcomingExDates(String kakaoUserId) {
        // MVP: 하드코딩된 응답
        // TODO: 실제 dividend-engine API 연동
        return "다가오는 배당락일\n" +
                "━━━━━━━━━━━━━━━\n" +
                "삼성전자 (005930)\n" +
                "  배당락일: 2026-06-25 (D-3)\n" +
                "  예상배당: 361원/주\n\n" +
                "※ 본 정보는 투자 조언이 아닙니다.";
    }

    public String getPortfolioSummary(String kakaoUserId) {
        // MVP: 하드코딩된 응답
        // TODO: 실제 dividend-engine API 연동
        return "나의 포트폴리오\n" +
                "━━━━━━━━━━━━━━━\n" +
                "삼성전자 100주\n" +
                "SK하이닉스 50주\n" +
                "━━━━━━━━━━━━━━━\n" +
                "총 2종목 보유";
    }
}
