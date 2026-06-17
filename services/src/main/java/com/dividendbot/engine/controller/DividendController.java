package com.dividendbot.engine.controller;

import com.dividendbot.engine.domain.entity.DividendInfo;
import com.dividendbot.engine.dto.MonthlyDividendSummaryDto;
import com.dividendbot.engine.service.DividendCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dividends")
@RequiredArgsConstructor
public class DividendController {

    private final DividendCalculationService dividendCalculationService;

    @GetMapping("/monthly")
    public ResponseEntity<MonthlyDividendSummaryDto> getMonthlyDividend(
            @RequestParam("user_id") UUID userId,
            @RequestParam("year") int year,
            @RequestParam("month") int month) {

        MonthlyDividendSummaryDto summary =
                dividendCalculationService.calculateMonthlyDividend(userId, year, month);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/ex-dates")
    public ResponseEntity<List<DividendInfo>> getUpcomingExDates(
            @RequestParam(value = "days_ahead", defaultValue = "7") int daysAhead) {

        List<DividendInfo> exDates =
                dividendCalculationService.findUpcomingExDividendDates(daysAhead);
        return ResponseEntity.ok(exDates);
    }
}
