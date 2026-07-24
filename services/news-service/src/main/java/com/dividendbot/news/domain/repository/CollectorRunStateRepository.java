package com.dividendbot.news.domain.repository;

import com.dividendbot.news.domain.entity.CollectorRunState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CollectorRunStateRepository extends JpaRepository<CollectorRunState, UUID> {
    Optional<CollectorRunState> findByCollectorName(String collectorName);
}
