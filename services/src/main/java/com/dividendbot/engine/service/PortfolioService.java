package com.dividendbot.engine.service;

import com.dividendbot.engine.domain.entity.Portfolio;
import com.dividendbot.engine.domain.repository.PortfolioRepository;
import com.dividendbot.engine.dto.PortfolioCreateDto;
import com.dividendbot.engine.dto.PortfolioResponseDto;
import com.dividendbot.engine.dto.PortfolioUpdateDto;
import com.dividendbot.engine.exception.PortfolioAccessDeniedException;
import com.dividendbot.engine.exception.PortfolioNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;

    /**
     * 인증된 사용자의 포트폴리오 목록을 exDividendDate ASC 정렬로 반환
     */
    public List<PortfolioResponseDto> getPortfolios(UUID userId) {
        List<Portfolio> portfolios = portfolioRepository.findByUserIdOrderByExDividendDateAsc(userId);
        return portfolios.stream()
                .map(this::toResponseDto)
                .toList();
    }

    /**
     * 종목 추가 (upsert 로직: 기존 stockCode면 update, 없으면 create)
     */
    @Transactional
    public PortfolioResponseDto addStock(UUID userId, PortfolioCreateDto dto) {
        Optional<Portfolio> existing = portfolioRepository
                .findByUserIdAndStockCode(userId, dto.stockCode());

        if (existing.isPresent()) {
            Portfolio portfolio = existing.get();
            portfolio.updateFields(
                    dto.quantity(),
                    dto.exDividendDate(),
                    dto.dividendPerShare()
            );
            log.info("Portfolio updated (upsert): user={}, stock={}, qty={}",
                    userId, dto.stockCode(), dto.quantity());
            Portfolio saved = portfolioRepository.save(portfolio);
            return toResponseDto(saved);
        }

        Portfolio portfolio = Portfolio.builder()
                .userId(userId)
                .stockCode(dto.stockCode())
                .stockName(dto.stockName())
                .quantity(dto.quantity())
                .exDividendDate(dto.exDividendDate())
                .dividendPerShare(dto.dividendPerShare() != null ? dto.dividendPerShare() : BigDecimal.ZERO)
                .build();

        log.info("Portfolio created: user={}, stock={}, qty={}",
                userId, dto.stockCode(), dto.quantity());
        Portfolio saved = portfolioRepository.save(portfolio);
        return toResponseDto(saved);
    }

    /**
     * 종목 수정: 소유권 검증 후 업데이트
     */
    @Transactional
    public PortfolioResponseDto updateStock(UUID userId, UUID portfolioId, PortfolioUpdateDto dto) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        if (!portfolio.getUserId().equals(userId)) {
            throw new PortfolioAccessDeniedException(portfolioId, userId);
        }

        portfolio.updateFields(
                dto.quantity(),
                dto.exDividendDate(),
                dto.dividendPerShare()
        );

        log.info("Portfolio updated: user={}, portfolioId={}, qty={}",
                userId, portfolioId, dto.quantity());
        Portfolio saved = portfolioRepository.save(portfolio);
        return toResponseDto(saved);
    }

    /**
     * 종목 삭제: 존재여부 검증 (404), 소유권 검증 (403) 후 삭제
     */
    @Transactional
    public void deleteStock(UUID userId, UUID portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        if (!portfolio.getUserId().equals(userId)) {
            throw new PortfolioAccessDeniedException(portfolioId, userId);
        }

        portfolioRepository.delete(portfolio);
        log.info("Portfolio deleted: user={}, portfolioId={}", userId, portfolioId);
    }

    private PortfolioResponseDto toResponseDto(Portfolio p) {
        BigDecimal expected = p.getDividendPerShare().multiply(BigDecimal.valueOf(p.getQuantity()));
        return new PortfolioResponseDto(
                p.getId(),
                p.getStockCode(),
                p.getStockName(),
                p.getQuantity(),
                p.getExDividendDate(),
                p.getDividendPerShare(),
                expected,
                p.getCreatedAt()
        );
    }
}
