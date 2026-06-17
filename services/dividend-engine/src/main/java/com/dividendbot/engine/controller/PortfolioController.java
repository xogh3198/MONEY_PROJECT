package com.dividendbot.engine.controller;

import com.dividendbot.engine.domain.entity.Portfolio;
import com.dividendbot.engine.dto.PortfolioRequestDto;
import com.dividendbot.engine.service.PortfolioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/portfolios")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping
    public ResponseEntity<List<Portfolio>> getPortfolios(@RequestParam("user_id") UUID userId) {
        return ResponseEntity.ok(portfolioService.getPortfolios(userId));
    }

    @PostMapping
    public ResponseEntity<Portfolio> addPortfolio(@Valid @RequestBody PortfolioRequestDto request) {
        Portfolio portfolio = portfolioService.addStock(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(portfolio);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removePortfolio(@PathVariable UUID id) {
        portfolioService.removeStock(id);
        return ResponseEntity.noContent().build();
    }
}
