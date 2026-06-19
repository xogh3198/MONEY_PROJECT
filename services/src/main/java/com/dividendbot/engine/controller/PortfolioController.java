package com.dividendbot.engine.controller;

import com.dividendbot.engine.dto.PortfolioCreateDto;
import com.dividendbot.engine.dto.PortfolioResponseDto;
import com.dividendbot.engine.dto.PortfolioUpdateDto;
import com.dividendbot.engine.service.PortfolioService;
import jakarta.servlet.http.HttpServletRequest;
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
@CrossOrigin(origins = "*")
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping
    public ResponseEntity<List<PortfolioResponseDto>> getPortfolios(HttpServletRequest request) {
        UUID userId = (UUID) request.getAttribute("userId");
        return ResponseEntity.ok(portfolioService.getPortfolios(userId));
    }

    @PostMapping
    public ResponseEntity<PortfolioResponseDto> addStock(HttpServletRequest request,
                                                         @Valid @RequestBody PortfolioCreateDto dto) {
        UUID userId = (UUID) request.getAttribute("userId");
        return ResponseEntity.status(HttpStatus.CREATED).body(portfolioService.addStock(userId, dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PortfolioResponseDto> updateStock(HttpServletRequest request,
                                                            @PathVariable UUID id,
                                                            @Valid @RequestBody PortfolioUpdateDto dto) {
        UUID userId = (UUID) request.getAttribute("userId");
        return ResponseEntity.ok(portfolioService.updateStock(userId, id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStock(HttpServletRequest request, @PathVariable UUID id) {
        UUID userId = (UUID) request.getAttribute("userId");
        portfolioService.deleteStock(userId, id);
        return ResponseEntity.noContent().build();
    }
}
