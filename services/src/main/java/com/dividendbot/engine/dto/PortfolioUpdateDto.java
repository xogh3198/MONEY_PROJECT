package com.dividendbot.engine.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PortfolioUpdateDto(
        @Min(value = 1, message = "수량은 1 이상이어야 합니다")
        int quantity,

        LocalDate exDividendDate,

        @DecimalMin(value = "0", message = "주당배당금은 0 이상이어야 합니다")
        BigDecimal dividendPerShare
) {
}
