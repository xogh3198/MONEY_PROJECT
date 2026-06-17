package com.dividendbot.gateway.service;

import com.dividendbot.gateway.dto.kakao.KakaoSkillRequest;
import com.dividendbot.gateway.dto.kakao.KakaoSkillResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 카카오 챗봇 Intent를 분석하여 적절한 서비스로 라우팅
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IntentRouterService {

    private final DividendEngineClient dividendEngineClient;

    public KakaoSkillResponse route(KakaoSkillRequest request) {
        String intent = request.getIntentName();

        return switch (intent) {
            case "dividend.monthly" -> handleMonthlyDividend(request);
            case "dividend.exdate" -> handleExDividendDate(request);
            case "portfolio.add" -> handlePortfolioAdd(request);
            case "portfolio.list" -> handlePortfolioList(request);
            case "account.isa_limit" -> handleISALimit(request);
            default -> handleFallback(request);
        };
    }

    private KakaoSkillResponse handleMonthlyDividend(KakaoSkillRequest request) {
        try {
            String result = dividendEngineClient.getMonthlyDividend(request.getUserId());
            return KakaoSkillResponse.simpleText(result);
        } catch (Exception e) {
            log.error("Monthly dividend error: {}", e.getMessage());
            return KakaoSkillResponse.simpleText("배당금 조회 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    private KakaoSkillResponse handleExDividendDate(KakaoSkillRequest request) {
        try {
            String result = dividendEngineClient.getUpcomingExDates(request.getUserId());
            return KakaoSkillResponse.simpleText(result);
        } catch (Exception e) {
            log.error("Ex-date query error: {}", e.getMessage());
            return KakaoSkillResponse.simpleText("배당락일 조회 중 오류가 발생했습니다.");
        }
    }

    private KakaoSkillResponse handlePortfolioAdd(KakaoSkillRequest request) {
        String utterance = request.getUtterance();
        // MVP: 간단한 파싱 ("삼성전자 100주 추가" → 종목명 + 수량)
        return KakaoSkillResponse.simpleText(
                "포트폴리오에 추가했습니다.\n" +
                "\"포트폴리오 보여줘\"로 현황을 확인하세요.\n\n" +
                "※ 본 정보는 투자 조언이 아닙니다.");
    }

    private KakaoSkillResponse handlePortfolioList(KakaoSkillRequest request) {
        try {
            String result = dividendEngineClient.getPortfolioSummary(request.getUserId());
            return KakaoSkillResponse.simpleText(result);
        } catch (Exception e) {
            log.error("Portfolio list error: {}", e.getMessage());
            return KakaoSkillResponse.simpleText("포트폴리오 조회 중 오류가 발생했습니다.");
        }
    }

    private KakaoSkillResponse handleISALimit(KakaoSkillRequest request) {
        return KakaoSkillResponse.simpleText(
                "ISA 비과세 한도 현황\n" +
                "━━━━━━━━━━━━━━━\n" +
                "계좌 유형: 일반형\n" +
                "비과세 한도: 2,000,000원\n" +
                "사용 금액: 조회 중...\n" +
                "잔여 한도: 조회 중...\n\n" +
                "※ 본 정보는 투자 조언이 아닙니다.");
    }

    private KakaoSkillResponse handleFallback(KakaoSkillRequest request) {
        return KakaoSkillResponse.withQuickReplies(
                "안녕하세요! 배당금 관리 봇입니다.\n무엇을 도와드릴까요?",
                java.util.List.of(
                        KakaoSkillResponse.QuickReply.builder()
                                .label("이번 달 배당금").messageText("이번 달 배당금").build(),
                        KakaoSkillResponse.QuickReply.builder()
                                .label("포트폴리오 보기").messageText("포트폴리오 보여줘").build(),
                        KakaoSkillResponse.QuickReply.builder()
                                .label("배당락일 확인").messageText("다음 배당락일").build()
                ));
    }
}
