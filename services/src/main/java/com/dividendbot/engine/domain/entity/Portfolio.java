package com.dividendbot.engine.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "portfolios", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "stock_code"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "stock_code", nullable = false, length = 10)
    private String stockCode;

    @Column(name = "stock_name", nullable = false, length = 50)
    private String stockName;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "ex_dividend_date")
    private LocalDate exDividendDate;

    @Column(name = "dividend_per_share", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal dividendPerShare = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public void updateQuantity(int newQuantity) {
        this.quantity = newQuantity;
    }

    public void updateFields(int quantity, LocalDate exDividendDate, BigDecimal dividendPerShare) {
        this.quantity = quantity;
        this.exDividendDate = exDividendDate;
        this.dividendPerShare = dividendPerShare != null ? dividendPerShare : BigDecimal.ZERO;
    }
}
