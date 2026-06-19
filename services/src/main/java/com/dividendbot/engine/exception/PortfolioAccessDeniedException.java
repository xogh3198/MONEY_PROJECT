package com.dividendbot.engine.exception;

import java.util.UUID;

public class PortfolioAccessDeniedException extends RuntimeException {

    public PortfolioAccessDeniedException(UUID portfolioId, UUID userId) {
        super("접근 권한이 없습니다: portfolioId=" + portfolioId + ", userId=" + userId);
    }
}
