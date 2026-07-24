package com.dividendbot.news.domain.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CollectorRunStateTest {

    @Test
    void recordsSuccessAfterPreviouslyBeingSkipped() {
        CollectorRunState state = CollectorRunState.create("NAVER_DATALAB", true, false);
        LocalDateTime attemptAt = LocalDateTime.of(2026, 7, 24, 10, 0);
        LocalDateTime successAt = attemptAt.plusSeconds(2);

        state.markSkipped(true, false, "credentials missing", attemptAt);
        state.markSuccess(50, 5, 2_000, "updated", successAt);

        assertThat(state.getStatus()).isEqualTo(CollectorRunStatus.SUCCESS);
        assertThat(state.isConfigured()).isTrue();
        assertThat(state.getProcessedCount()).isEqualTo(50);
        assertThat(state.getAvailableCount()).isEqualTo(5);
        assertThat(state.getLastSuccessAt()).isEqualTo(successAt);
    }

    @Test
    void sanitizesAndTruncatesDiagnosticMessages() {
        CollectorRunState state = CollectorRunState.create("EXTERNAL_ENGAGEMENT", true, true);
        String unsafeMessage = "line one\nline two\t" + "x".repeat(600);

        state.markFailed(12, unsafeMessage, LocalDateTime.now());

        assertThat(state.getMessage()).doesNotContain("\n", "\t");
        assertThat(state.getMessage()).hasSize(500);
        assertThat(state.getStatus()).isEqualTo(CollectorRunStatus.FAILED);
    }
}
