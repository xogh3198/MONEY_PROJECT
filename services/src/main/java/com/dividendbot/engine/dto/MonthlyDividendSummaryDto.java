package com.dividendbot.engine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyDividendSummaryDto {
    private String month;
    private BigDecimal totalPreTax;
    private BigDecimal totalAfterTax;
    private BigDecimal totalTax;
    private List<DividendResultDto> items;

    public static MonthlyDividendSummaryDto empty(int year, int month) {
        return MonthlyDividendSummaryDto.builder()
                .month(String.format("%d-%02d", year, month))
                .totalPreTax(BigDecimal.ZERO)
                .totalAfterTax(BigDecimal.ZERO)
                .totalTax(BigDecimal.ZERO)
                .items(Collections.emptyList())
                .build();
    }
}
