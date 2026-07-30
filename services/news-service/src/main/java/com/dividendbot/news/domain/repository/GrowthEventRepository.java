package com.dividendbot.news.domain.repository;

import com.dividendbot.news.domain.entity.GrowthEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface GrowthEventRepository extends JpaRepository<GrowthEvent, Long> {
    long countByVisitorIdHashAndCreatedAtAfter(String visitorIdHash, LocalDateTime createdAt);

    Page<GrowthEvent> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime createdAt, Pageable pageable);

    long deleteByCreatedAtBefore(LocalDateTime createdAt);
}
