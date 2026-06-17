package com.dividendbot.engine.infra.kis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KisDividendResponse {
    private String stockCode;
    private String stockName;
    private BigDecimal currentPrice;
    private BigDecimal dividendPerShare;
    private BigDecimal dividendYield;
    private boolean success;

    public static KisDividendResponse from(Map<String, Object> response) {
        if (response == null) return empty("UNKNOWN");

        try {
            Map<String, Object> output = (Map<String, Object>) response.get("output");
            if (output == null) return empty("UNKNOWN");

            return KisDividendResponse.builder()
                    .stockCode(String.valueOf(output.getOrDefault("stck_shrn_iscd", "")))
                    .stockName(String.valueOf(output.getOrDefault("hts_kor_isnm", "")))
                    .currentPrice(new BigDecimal(String.valueOf(output.getOrDefault("stck_prpr", "0"))))
                    .dividendPerShare(BigDecimal.ZERO) // 배당 전용 API에서 별도 조회
                    .dividendYield(BigDecimal.ZERO)
                    .success(true)
                    .build();
        } catch (Exception e) {
            return empty("PARSE_ERROR");
        }
    }

    public static KisDividendResponse empty(String stockCode) {
        return KisDividendResponse.builder()
                .stockCode(stockCode)
                .stockName("")
                .currentPrice(BigDecimal.ZERO)
                .dividendPerShare(BigDecimal.ZERO)
                .dividendYield(BigDecimal.ZERO)
                .success(false)
                .build();
    }
}
