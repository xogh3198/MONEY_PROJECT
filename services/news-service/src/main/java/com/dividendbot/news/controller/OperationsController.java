package com.dividendbot.news.controller;

import com.dividendbot.news.dto.OperationsStatusResponse;
import com.dividendbot.news.service.OperationsStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operations")
@RequiredArgsConstructor
public class OperationsController {

    private final OperationsStatusService operationsStatusService;

    @GetMapping("/status")
    public ResponseEntity<OperationsStatusResponse> getStatus() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(operationsStatusService.getStatus());
    }
}
