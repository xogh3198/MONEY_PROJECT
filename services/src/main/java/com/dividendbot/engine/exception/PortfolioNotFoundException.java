package com.dividendbot.engine.exception;

import java.util.UUID;

public class PortfolioNotFoundException extends RuntimeException {

    public PortfolioNotFoundException(UUID portfolioId) {
        super("포트폴리오 항목을 찾을 수 없습니다: " + portfolioId);
    }
}
