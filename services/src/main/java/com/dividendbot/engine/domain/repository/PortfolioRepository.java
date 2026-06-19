package com.dividendbot.engine.domain.repository;

import com.dividendbot.engine.domain.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioRepository extends JpaRepository<Portfolio, UUID> {
    List<Portfolio> findByUserId(UUID userId);
    List<Portfolio> findByUserIdOrderByExDividendDateAsc(UUID userId);
    Optional<Portfolio> findByUserIdAndStockCode(UUID userId, String stockCode);
    List<Portfolio> findByExDividendDate(LocalDate exDividendDate);
}
