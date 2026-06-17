package com.dividendbot.engine.domain.repository;

import com.dividendbot.engine.domain.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioRepository extends JpaRepository<Portfolio, UUID> {
    List<Portfolio> findByUserId(UUID userId);
    Optional<Portfolio> findByUserIdAndStockCode(UUID userId, String stockCode);
}
