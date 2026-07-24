package com.dividendbot.news.service;

import com.dividendbot.news.domain.entity.CollectorRunState;
import com.dividendbot.news.domain.repository.CollectorRunStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CollectorRunStateService {
    public static final String NAVER_DATALAB = "NAVER_DATALAB";
    public static final String EXTERNAL_ENGAGEMENT = "EXTERNAL_ENGAGEMENT";

    private final CollectorRunStateRepository repository;

    @Transactional
    public void markRunning(String name, boolean enabled, boolean configured) {
        CollectorRunState state = getOrCreate(name, enabled, configured);
        state.markRunning(enabled, configured, LocalDateTime.now());
        repository.save(state);
    }

    @Transactional
    public void markSuccess(String name, int processed, int available, long durationMs, String message) {
        CollectorRunState state = getOrCreate(name, true, true);
        state.markSuccess(processed, available, durationMs, message, LocalDateTime.now());
        repository.save(state);
    }

    @Transactional
    public void markSkipped(String name, boolean enabled, boolean configured, String message) {
        CollectorRunState state = getOrCreate(name, enabled, configured);
        state.markSkipped(enabled, configured, message, LocalDateTime.now());
        repository.save(state);
    }

    @Transactional
    public void markFailed(String name, long durationMs, String message) {
        CollectorRunState state = getOrCreate(name, true, true);
        state.markFailed(durationMs, message, LocalDateTime.now());
        repository.save(state);
    }

    @Transactional(readOnly = true)
    public List<CollectorRunState> findAll() {
        return repository.findAll().stream()
                .sorted((left, right) -> left.getCollectorName().compareTo(right.getCollectorName()))
                .toList();
    }

    private CollectorRunState getOrCreate(String name, boolean enabled, boolean configured) {
        return repository.findByCollectorName(name)
                .orElseGet(() -> CollectorRunState.create(name, enabled, configured));
    }
}
