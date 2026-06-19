package com.dividendbot.engine.domain.repository;

import com.dividendbot.engine.domain.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {
    Optional<NotificationPreference> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
}
