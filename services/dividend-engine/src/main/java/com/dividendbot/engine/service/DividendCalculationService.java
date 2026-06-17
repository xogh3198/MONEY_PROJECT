package com.dividendbot.engine.service;

import com.dividendbot.engine.config.TaxRateConfig;
import com.dividendbot.engine.domain.entity.AccountType;
import com.dividendbot.engine.domain.entity.DividendInfo;
import com.dividendbot.engine.domain.entity.Portfolio;
import com.dividendbot.engine.domain.repository.DividendInfoRepository;
import com.dividendbot.engine.domain.repository.PortfolioRepository;
import com.dividendbot.engine.dto.DividendResultDto;
import com.dividendbot.engine.dto.MonthlyDividendSummaryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DividendCalculationService {

    private final PortfolioRepository portfolioRepository;
    private final DividendInfoRepository dividendInfoRepository;
    private final TaxRateConfig taxRateConfig;

    /**
     * 월별 배당금 계산
     * - 세율은 TaxRateConfig에서 로드 (하드코딩 금지)
     * - 금액은 BigDecimal 사용 (float 금지)
     * - 원 단위 이하 절사 (Floor)
     */
    public MonthlyDividendSummaryDto calculateMonthlyDividend(UUID userId, int year, int month) {
        List<Portfolio> portfolios = portfolioRepository.findByUserId(userId);
        if (portfolios.isEmpty()) {
            return MonthlyDividendSummaryDto.empty(year, month);
        }

        List<String> stockCodes = portfolios.stream()
                .map(Portfolio::getStockCode)
                .collect(Collectors.toList());

        List<DividendInfo> dividends = dividendInfoRepository.findByStockCodesAndYear(stockCodes, year);

        // 해당 월의 배당만 필터링 (payment_date 기준)
        List<DividendInfo> monthlyDividends = dividends.stream()
                .filter(d -> d.getPaymentDate() != null && d.getPaymentDate().getMonthValue() == month)
                .collect(Collectors.toList());

        List<DividendResultDto> results = monthlyDividends.stream()
                .map(div -> {
                    Portfolio portfolio = portfolios.stream()
                            .filter(p -> p.getStockCode().equals(div.getStockCode()))
                            .findFirst()
                            .orElse(null);
                    if (portfolio == null) return null;
                    return calculateSingleDividend(div, portfolio, AccountType.GENERAL);
                })
                .filter(r -> r != null)
                .collect(Collectors.toList());

        return buildSummary(year, month, results);
    }

    /**
     * 단일 종목 배당금 계산 (세전/세후)
     */
    public DividendResultDto calculateSingleDividend(
            DividendInfo dividendInfo, Portfolio portfolio, AccountType accountType) {

        BigDecimal preTax = dividendInfo.getDividendPerShare()
                .multiply(BigDecimal.valueOf(portfolio.getQuantity()));

        BigDecimal taxRate = resolveTaxRate(accountType, preTax);
        BigDecimal taxAmount = preTax.multiply(taxRate).setScale(0, RoundingMode.FLOOR);
        BigDecimal afterTax = preTax.subtract(taxAmount);

        return DividendResultDto.builder()
                .stockCode(portfolio.getStockCode())
                .stockName(portfolio.getStockName())
                .quantity(portfolio.getQuantity())
                .preTax(preTax.setScale(0, RoundingMode.FLOOR))
                .afterTax(afterTax.setScale(0, RoundingMode.FLOOR))
                .taxAmount(taxAmount)
                .taxRate(taxRate)
                .accountType(accountType)
                .build();
    }

    /**
     * 배당락일 N일 이내 종목 조회
     */
    public List<DividendInfo> findUpcomingExDividendDates(int daysAhead) {
        LocalDate today = LocalDate.now();
        LocalDate end = today.plusDays(daysAhead);
        return dividendInfoRepository.findByExDividendDateBetween(today, end);
    }

    /**
     * 계좌 유형에 따른 세율 결정
     * - GENERAL: 15.4% 원천징수
     * - ISA: 비과세 한도 내 0%, 초과분 9.9%
     * - IRP: 0% (과세이연)
     */
    private BigDecimal resolveTaxRate(AccountType accountType, BigDecimal preTax) {
        return switch (accountType) {
            case GENERAL -> taxRateConfig.getDividend().getGeneralWithholdingRate();
            case ISA_GENERAL, ISA_SPECIAL -> {
                BigDecimal limit = accountType == AccountType.ISA_GENERAL
                        ? taxRateConfig.getIsa().getGeneralTaxFreeLimit()
                        : taxRateConfig.getIsa().getSpecialTaxFreeLimit();
                // MVP 단계: 단순 비교 (누적 추적은 Phase 2)
                if (preTax.compareTo(limit) <= 0) {
                    yield BigDecimal.ZERO;
                } else {
                    yield taxRateConfig.getIsa().getExcessTaxRate();
                }
            }
            case IRP -> BigDecimal.ZERO; // 과세이연
        };
    }

    private MonthlyDividendSummaryDto buildSummary(int year, int month, List<DividendResultDto> results) {
        BigDecimal totalPreTax = results.stream()
                .map(DividendResultDto::getPreTax)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAfterTax = results.stream()
                .map(DividendResultDto::getAfterTax)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalTax = results.stream()
                .map(DividendResultDto::getTaxAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return MonthlyDividendSummaryDto.builder()
                .month(String.format("%d-%02d", year, month))
                .totalPreTax(totalPreTax)
                .totalAfterTax(totalAfterTax)
                .totalTax(totalTax)
                .items(results)
                .build();
    }
}
