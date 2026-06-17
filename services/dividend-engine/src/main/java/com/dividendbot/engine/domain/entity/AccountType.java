package com.dividendbot.engine.domain.entity;

public enum AccountType {
    GENERAL,        // 일반 계좌 (원천징수 15.4%)
    ISA_GENERAL,    // ISA 일반형 (비과세 200만원, 초과 9.9%)
    ISA_SPECIAL,    // ISA 서민형 (비과세 400만원, 초과 9.9%)
    IRP             // IRP (과세이연)
}
