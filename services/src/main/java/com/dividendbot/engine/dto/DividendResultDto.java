package com.dividendbot.engine.dto;

import com.dividendbot.engine.domain.entity.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DividendResultDto {
    private String stockCode;
    private String stockName;
    private int quantity;
    private BigDecimal preTax;
    private BigDecimal afterTax;
    private BigDecimal taxAmount;
    private BigDecimal taxRate;
    private AccountType accountType;
}
