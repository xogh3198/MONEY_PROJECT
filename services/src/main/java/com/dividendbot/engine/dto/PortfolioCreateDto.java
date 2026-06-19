package com.dividendbot.engine.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PortfolioCreateDto(
        @NotBlank(message = "종목코드는 필수입니다")
        @Size(max = 10, message = "종목코드는 최대 10자까지 가능합니다")
        String stockCode,

        @NotBlank(message = "종목명은 필수입니다")
        String stockName,

        @Min(value = 1, message = "수량은 1 이상이어야 합니다")
        int quantity,

        LocalDate exDividendDate,

        @DecimalMin(value = "0", message = "주당배당금은 0 이상이어야 합니다")
        BigDecimal dividendPerShare
) {
}
