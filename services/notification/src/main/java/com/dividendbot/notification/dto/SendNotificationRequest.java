package com.dividendbot.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationRequest {

    @NotNull
    private UUID userId;

    @NotBlank
    private String type; // EX_DATE_D3, EX_DATE_D1, ISA_LIMIT, MONTHLY_REPORT

    @NotBlank
    private String message;
}
