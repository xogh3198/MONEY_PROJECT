package com.dividendbot.engine.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * ISA/IRP 계좌의 연간 누적 배당소득 추적
 * 비과세 한도 판단에 사용
 */
@Entity
@Table(name = "user_dividend_accumulation",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "year"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DividendAccumulation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private int year;

    @Column(name = "accumulated_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal accumulatedAmount = BigDecimal.ZERO;

    /**
     * 배당금 수령 시 누적액 증가
     */
    public void addDividend(BigDecimal amount) {
        this.accumulatedAmount = this.accumulatedAmount.add(amount);
    }

    /**
     * 비과세 한도 초과 여부 확인
     */
    public boolean exceedsLimit(BigDecimal limit) {
        return this.accumulatedAmount.compareTo(limit) > 0;
    }

    /**
     * 이번 배당 추가 시 한도 초과분 계산
     * @return 초과분 (0 이상), 한도 내이면 ZERO
     */
    public BigDecimal calculateExcess(BigDecimal newDividend, BigDecimal limit) {
        BigDecimal afterAdd = this.accumulatedAmount.add(newDividend);
        if (afterAdd.compareTo(limit) <= 0) {
            return BigDecimal.ZERO;
        }
        // 기존 누적이 이미 한도 초과인 경우: 전액 과세
        if (this.accumulatedAmount.compareTo(limit) >= 0) {
            return newDividend;
        }
        // 이번 배당으로 한도 돌파: 초과분만 과세
        return afterAdd.subtract(limit);
    }
}
