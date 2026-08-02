package com.dividendbot.news.service.video;

import com.dividendbot.news.dto.ReferenceVideoAnalysisRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApifyYouTubeReferenceServiceTest {

    @Test
    void extractsSupportedYouTubeVideoIds() {
        assertThat(ApifyYouTubeReferenceService.extractVideoId("https://youtu.be/jNQXAC9IVRw")).contains("jNQXAC9IVRw");
        assertThat(ApifyYouTubeReferenceService.extractVideoId("https://www.youtube.com/watch?v=jNQXAC9IVRw&t=1")).contains("jNQXAC9IVRw");
        assertThat(ApifyYouTubeReferenceService.extractVideoId("https://www.youtube.com/shorts/jNQXAC9IVRw")).contains("jNQXAC9IVRw");
        assertThat(ApifyYouTubeReferenceService.extractVideoId("https://example.com/watch?v=jNQXAC9IVRw")).isEmpty();
    }

    @Test
    void keepsDraftFlowAvailableWhenApifyIsNotConfigured() {
        ApifyYouTubeReferenceService service = new ApifyYouTubeReferenceService(
                new ObjectMapper(),
                "",
                "apihq~youtube-transcript-scraper",
                false
        );

        var response = service.analyze(new ReferenceVideoAnalysisRequest(
                "https://youtu.be/jNQXAC9IVRw",
                "첫 2초에 질문하고 빠르게 전환"
        ));

        assertThat(response.status()).isEqualTo("NOT_CONFIGURED");
        assertThat(response.styleSummary()).contains("첫 2초에 질문");
        assertThat(response.styleSummary()).contains("복제하지 않습니다");
    }
}
