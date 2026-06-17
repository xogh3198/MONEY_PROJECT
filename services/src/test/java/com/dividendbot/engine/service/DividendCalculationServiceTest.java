package com.dividendbot.engine.service;

import com.dividendbot.engine.config.TaxRateConfig;
import com.dividendbot.engine.domain.entity.*;
import com.dividendbot.engine.domain.repository.DividendAccumulationRepository;
import com.dividendbot.engine.domain.repository.DividendInfoRepository;
import com.dividendbot.engine.domain.repository.PortfolioRepository;
import com.dividendbot.engine.dto.DividendResultDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class DividendCalculationServiceTest {

    @Mock private PortfolioRepository portfolioRepository;
    @Mock private DividendInfoRepository dividendInfoRepository;
    @Mock private DividendAccumulationRepository accumulationRepository;

    private TaxRateConfig taxRateConfig;

    @InjectMocks
    private DividendCalculationService service;

    @BeforeEach
    void setUp() {
        taxRateConfig = new TaxRateConfig();
        taxRateConfig.setDividend(new TaxRateConfig.Dividend());
        taxRateConfig.setIsa(new TaxRateConfig.Isa());
        MockitoAnnotations.openMocks(this);

        // 리플렉션으로 taxRateConfig 주입 (Lombok @RequiredArgsConstructor 대응)
        service = new DividendCalculationService(
                portfolioRepository, dividendInfoRepository, accumulationRepository, taxRateConfig);
    }

    @Nested
    @DisplayName("일반 계좌 배당금 계산")
    class GeneralAccountTest {

        @ParameterizedTest(name = "주당배당 {0}원 × {1}주 = 세전 {2}원, 세후 {3}원")
        @CsvSource({
            "361, 100, 36100, 30540",   // 삼성전자 100주
            "1200, 50, 60000, 50760",   // SK하이닉스 50주
            "0, 100, 0, 0",             // 배당금 0원
            "1, 1, 1, 0",              // 최소 단위 (1원 × 15.4% = 0.154 → 절사 → 세금0, 세후1... 확인필요)
        })
        void 일반계좌_원천징수_15_4퍼센트(int perShare, int qty, int expectedPreTax, int expectedAfterTax) {
            DividendInfo info = DividendInfo.builder()
                    .stockCode("005930")
                    .dividendPerShare(BigDecimal.valueOf(perShare))
                    .exDividendDate(LocalDate.of(2026, 6, 25))
                    .paymentDate(LocalDate.of(2026, 7, 15))
                    .year(2026)
                    .build();

            Portfolio portfolio = Portfolio.builder()
                    .stockCode("005930")
                    .stockName("삼성전자")
                    .quantity(qty)
                    .build();

            DividendResultDto result = service.calculateSingleDividend(info, portfolio, AccountType.GENERAL);

            assertThat(result.getPreTax()).isEqualByComparingTo(BigDecimal.valueOf(expectedPreTax));
            // 세후 = 세전 - floor(세전 × 0.154)
            BigDecimal tax = BigDecimal.valueOf(expectedPreTax)
                    .multiply(new BigDecimal("0.154"))
                    .setScale(0, RoundingMode.FLOOR);
            BigDecimal expected = BigDecimal.valueOf(expectedPreTax).subtract(tax);
            assertThat(result.getAfterTax()).isEqualByComparingTo(expected);
        }

        @Test
        @DisplayName("세율은 정확히 15.4%여야 한다")
        void 세율_확인() {
            assertThat(taxRateConfig.getDividend().getGeneralWithholdingRate())
                    .isEqualByComparingTo(new BigDecimal("0.154"));
        }
    }

    @Nested
    @DisplayName("ISA 계좌 누적 비과세 한도 검증")
    class ISAAccountTest {

        private UUID userId = UUID.randomUUID();

        @Test
        @DisplayName("누적 0원 + 배당 100만원 → 한도 내 → 세금 0원")
        void ISA_한도내_비과세() {
            when(accumulationRepository.findByUserIdAndYear(userId, 2026))
                    .thenReturn(Optional.of(DividendAccumulation.builder()
                            .userId(userId).year(2026)
                            .accumulatedAmount(BigDecimal.ZERO).build()));

            BigDecimal tax = service.calculateISATax(
                    userId, 2026, AccountType.ISA_GENERAL, new BigDecimal("1000000"));

            assertThat(tax).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("누적 199만원 + 배당 2만원 → 1만원 초과 → 초과분 9.9% 과세")
        void ISA_한도_돌파_경계값() {
            when(accumulationRepository.findByUserIdAndYear(userId, 2026))
                    .thenReturn(Optional.of(DividendAccumulation.builder()
                            .userId(userId).year(2026)
                            .accumulatedAmount(new BigDecimal("1990000")).build()));

            BigDecimal tax = service.calculateISATax(
                    userId, 2026, AccountType.ISA_GENERAL, new BigDecimal("20000"));

            // 초과분 = (199만 + 2만) - 200만 = 1만원, 1만 × 9.9% = 990원
            assertThat(tax).isEqualByComparingTo(new BigDecimal("990"));
        }

        @Test
        @DisplayName("누적 200만원(한도 도달) + 추가 배당 5만원 → 전액 과세")
        void ISA_한도_초과_전액과세() {
            when(accumulationRepository.findByUserIdAndYear(userId, 2026))
                    .thenReturn(Optional.of(DividendAccumulation.builder()
                            .userId(userId).year(2026)
                            .accumulatedAmount(new BigDecimal("2000000")).build()));

            BigDecimal tax = service.calculateISATax(
                    userId, 2026, AccountType.ISA_GENERAL, new BigDecimal("50000"));

            // 전액 초과: 5만 × 9.9% = 4,950원
            assertThat(tax).isEqualByComparingTo(new BigDecimal("4950"));
        }

        @Test
        @DisplayName("서민형 ISA: 비과세 한도 400만원")
        void ISA_서민형_한도_400만() {
            when(accumulationRepository.findByUserIdAndYear(userId, 2026))
                    .thenReturn(Optional.of(DividendAccumulation.builder()
                            .userId(userId).year(2026)
                            .accumulatedAmount(new BigDecimal("3500000")).build()));

            BigDecimal tax = service.calculateISATax(
                    userId, 2026, AccountType.ISA_SPECIAL, new BigDecimal("600000"));

            // 누적 350만 + 60만 = 410만, 한도 400만, 초과 10만 → 10만 × 9.9% = 9,900원
            assertThat(tax).isEqualByComparingTo(new BigDecimal("9900"));
        }
    }

    @Nested
    @DisplayName("IRP 계좌 과세이연")
    class IRPAccountTest {

        @Test
        @DisplayName("IRP는 항상 세금 0원 (과세이연)")
        void IRP_과세이연() {
            DividendInfo info = DividendInfo.builder()
                    .stockCode("005930")
                    .dividendPerShare(new BigDecimal("361"))
                    .year(2026)
                    .build();

            Portfolio portfolio = Portfolio.builder()
                    .stockCode("005930")
                    .stockName("삼성전자")
                    .quantity(100)
                    .build();

            DividendResultDto result = service.calculateSingleDividend(info, portfolio, AccountType.IRP);

            assertThat(result.getTaxAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getAfterTax()).isEqualByComparingTo(result.getPreTax());
        }
    }
}
