package com.dividendbot.engine.domain.repository;

import com.dividendbot.engine.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByKakaoUserId(String kakaoUserId);
}
