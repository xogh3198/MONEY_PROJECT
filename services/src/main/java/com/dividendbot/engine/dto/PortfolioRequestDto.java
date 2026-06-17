package com.dividendbot.engine.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioRequestDto {

    @NotNull(message = "사용자 ID는 필수입니다")
    private UUID userId;

    @NotBlank(message = "종목코드는 필수입니다")
    private String stockCode;

    @NotBlank(message = "종목명은 필수입니다")
    private String stockName;

    @Min(value = 1, message = "수량은 1 이상이어야 합니다")
    private int quantity;
}
