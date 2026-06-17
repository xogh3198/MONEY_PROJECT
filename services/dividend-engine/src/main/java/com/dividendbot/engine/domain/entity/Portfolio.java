package com.dividendbot.engine.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "portfolios")
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

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public void updateQuantity(int newQuantity) {
        this.quantity = newQuantity;
    }
}
