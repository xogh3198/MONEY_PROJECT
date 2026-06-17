package com.dividendbot.engine.service;

import com.dividendbot.engine.domain.entity.Portfolio;
import com.dividendbot.engine.domain.repository.PortfolioRepository;
import com.dividendbot.engine.dto.PortfolioRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;

    public List<Portfolio> getPortfolios(UUID userId) {
        return portfolioRepository.findByUserId(userId);
    }

    @Transactional
    public Portfolio addStock(PortfolioRequestDto request) {
        // 이미 보유한 종목이면 수량 업데이트
        Optional<Portfolio> existing = portfolioRepository
                .findByUserIdAndStockCode(request.getUserId(), request.getStockCode());

        if (existing.isPresent()) {
            Portfolio portfolio = existing.get();
            portfolio.updateQuantity(request.getQuantity());
            log.info("Portfolio updated: user={}, stock={}, qty={}",
                    request.getUserId(), request.getStockCode(), request.getQuantity());
            return portfolioRepository.save(portfolio);
        }

        Portfolio portfolio = Portfolio.builder()
                .userId(request.getUserId())
                .stockCode(request.getStockCode())
                .stockName(request.getStockName())
                .quantity(request.getQuantity())
                .build();

        log.info("Portfolio created: user={}, stock={}, qty={}",
                request.getUserId(), request.getStockCode(), request.getQuantity());
        return portfolioRepository.save(portfolio);
    }

    @Transactional
    public void removeStock(UUID portfolioId) {
        portfolioRepository.deleteById(portfolioId);
        log.info("Portfolio removed: id={}", portfolioId);
    }
}
