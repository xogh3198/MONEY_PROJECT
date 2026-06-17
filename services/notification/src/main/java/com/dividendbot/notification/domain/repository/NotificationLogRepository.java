package com.dividendbot.notification.domain.repository;

import com.dividendbot.notification.domain.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {
    List<NotificationLog> findByUserIdOrderBySentAtDesc(UUID userId);
    long countBySentAtBetween(LocalDateTime start, LocalDateTime end);
}
