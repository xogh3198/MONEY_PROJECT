package com.dividendbot.news.controller;

import com.dividendbot.news.service.video.AiSceneGenerationService;
import com.dividendbot.news.service.video.ApifyYouTubeReferenceService;
import com.dividendbot.news.service.video.ShortformSourceStorage;
import com.dividendbot.news.service.video.VideoAssetStorage;
import com.dividendbot.news.service.video.VideoRenderAccessGuard;
import com.dividendbot.news.service.video.VideoRenderService;
import com.dividendbot.news.service.video.YouTubeDiscoveryService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ContentVideoControllerShortformUploadTest {
    @Test
    void rejectsOversizedChunkBeforeReadingRequestBody() throws Exception {
        VideoRenderAccessGuard guard = mock(VideoRenderAccessGuard.class);
        ShortformSourceStorage sources = mock(ShortformSourceStorage.class);
        ContentVideoController controller = new ContentVideoController(guard, mock(VideoAssetStorage.class),
                mock(VideoRenderService.class), mock(AiSceneGenerationService.class),
                mock(ApifyYouTubeReferenceService.class), mock(YouTubeDiscoveryService.class), sources);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContentLengthLong()).thenReturn((long) ShortformSourceStorage.MAX_CHUNK_BYTES + 1);

        assertThatThrownBy(() -> controller.uploadShortformChunk("key", UUID.randomUUID(), 0, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("413");
        verify(guard).requireAuthorized("key");
        verify(request, never()).getInputStream();
        verifyNoInteractions(sources);
    }
}
