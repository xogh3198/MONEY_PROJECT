package com.dividendbot.engine.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "dividend_info")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DividendInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "stock_code", nullable = false, length = 10)
    private String stockCode;

    @Column(name = "dividend_per_share", nullable = false, precision = 15, scale = 2)
    private BigDecimal dividendPerShare;

    @Column(name = "ex_dividend_date")
    private LocalDate exDividendDate;

    @Column(name = "record_date")
    private LocalDate recordDate;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "dividend_yield", precision = 5, scale = 4)
    private BigDecimal dividendYield;

    @Column(nullable = false)
    private int year;
}
