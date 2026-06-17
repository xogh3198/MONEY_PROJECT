package com.dividendbot.engine.config;

import com.dividendbot.engine.domain.entity.AccountType;
import com.dividendbot.engine.domain.entity.User;
import com.dividendbot.engine.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.util.Base64;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail("admin@money.com")) {
            return;
        }

        User admin = User.builder()
                .email("admin@money.com")
                .password(hashPassword("admin1234"))
                .nickname("관리자")
                .accountType(AccountType.GENERAL)
                .role("ADMIN")
                .build();

        userRepository.save(admin);
        log.info("관리자 계정 생성 완료: admin@money.com");
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
