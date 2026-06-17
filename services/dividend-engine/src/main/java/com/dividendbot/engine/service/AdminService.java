package com.dividendbot.engine.service;

import com.dividendbot.engine.domain.repository.DividendInfoRepository;
import com.dividendbot.engine.domain.repository.PortfolioRepository;
import com.dividendbot.engine.domain.repository.UserRepository;
import com.dividendbot.engine.dto.AdminStatsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final DividendInfoRepository dividendInfoRepository;

    public AdminStatsDto getStats() {
        return AdminStatsDto.builder()
                .totalUsers(userRepository.count())
                .totalPortfolios(portfolioRepository.count())
                .dividendDataCount(dividendInfoRepository.count())
                .notificationsSentToday(0) // TODO: notification 서비스 연동 후 구현
                .build();
    }
}
