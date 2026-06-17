package com.dividendbot.engine.domain.repository;

import com.dividendbot.engine.domain.entity.DividendAccumulation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface DividendAccumulationRepository extends JpaRepository<DividendAccumulation, UUID> {
    Optional<DividendAccumulation> findByUserIdAndYear(UUID userId, int year);
}
