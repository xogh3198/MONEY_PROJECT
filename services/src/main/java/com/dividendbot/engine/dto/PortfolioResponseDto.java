package com.dividendbot.engine.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PortfolioResponseDto(
        UUID id,
        String stockCode,
        String stockName,
        int quantity,
        LocalDate exDividendDate,
        BigDecimal dividendPerShare,
        BigDecimal expectedDividend,
        LocalDateTime createdAt
) {
}
