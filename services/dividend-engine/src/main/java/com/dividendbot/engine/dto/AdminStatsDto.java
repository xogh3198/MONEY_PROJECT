package com.dividendbot.engine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminStatsDto {
    private long totalUsers;
    private long totalPortfolios;
    private long notificationsSentToday;
    private long dividendDataCount;
}
