package com.dividendbot.news.domain.entity;

public enum ExternalMetricStatus {
    PENDING,
    AVAILABLE,
    NOT_SUPPORTED,
    NOT_CONFIGURED,
    BLOCKED_BY_POLICY,
    ROBOTS_UNAVAILABLE,
    INVALID_URL,
    FETCH_ERROR
}
