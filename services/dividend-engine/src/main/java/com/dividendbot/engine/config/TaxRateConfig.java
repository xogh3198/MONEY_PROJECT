package com.dividendbot.engine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.math.BigDecimal;

/**
 * 세율 설정 - application.yml에서 로드
 * 절대 하드코딩 금지. 세법 변경 시 설정 파일만 수정.
 */
@Configuration
@ConfigurationProperties(prefix = "tax")
@Getter
@Setter
public class TaxRateConfig {

    private Dividend dividend = new Dividend();
    private Isa isa = new Isa();

    @Getter
    @Setter
    public static class Dividend {
        /** 일반 계좌 원천징수율 (15.4%) */
        private BigDecimal generalWithholdingRate = new BigDecimal("0.154");
    }

    @Getter
    @Setter
    public static class Isa {
        /** ISA 일반형 비과세 한도 (200만원) */
        private BigDecimal generalTaxFreeLimit = new BigDecimal("2000000");
        /** ISA 서민형 비과세 한도 (400만원) */
        private BigDecimal specialTaxFreeLimit = new BigDecimal("4000000");
        /** ISA 비과세 초과분 세율 (9.9%) */
        private BigDecimal excessTaxRate = new BigDecimal("0.099");
    }
}
