package com.dividendbot.news.controller;

import com.dividendbot.news.dto.VideoRenderJobResponse;
import com.dividendbot.news.dto.VideoRenderRequest;
import com.dividendbot.news.service.video.VideoRenderAccessGuard;
import com.dividendbot.news.service.video.VideoRenderService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/content-videos")
public class ContentVideoController {

    private final VideoRenderAccessGuard accessGuard;
    private final VideoRenderService videoRenderService;

    public ContentVideoController(
            VideoRenderAccessGuard accessGuard,
            VideoRenderService videoRenderService
    ) {
        this.accessGuard = accessGuard;
        this.videoRenderService = videoRenderService;
    }

    @PostMapping("/render")
    public ResponseEntity<VideoRenderJobResponse> render(
            @RequestHeader(value = "X-Video-Render-Key", required = false) String accessKey,
            @Valid @RequestBody VideoRenderRequest request
    ) {
        accessGuard.requireAuthorized(accessKey);
        return ResponseEntity.accepted()
                .cacheControl(CacheControl.noStore())
                .body(videoRenderService.submit(request));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<VideoRenderJobResponse> get(
            @RequestHeader(value = "X-Video-Render-Key", required = false) String accessKey,
            @PathVariable UUID jobId
    ) {
        accessGuard.requireAuthorized(accessKey);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(videoRenderService.get(jobId));
    }

    @GetMapping("/{jobId}/file")
    public ResponseEntity<Resource> file(
            @RequestHeader(value = "X-Video-Render-Key", required = false) String accessKey,
            @PathVariable UUID jobId
    ) {
        accessGuard.requireAuthorized(accessKey);
        Resource file = videoRenderService.getFile(jobId);
        String fileName = videoRenderService.fileName(jobId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType("video/mp4"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(file);
    }

    @GetMapping("/capabilities")
    public ResponseEntity<Map<String, Object>> capabilities(
            @RequestHeader(value = "X-Video-Render-Key", required = false) String accessKey
    ) {
        accessGuard.requireAuthorized(accessKey);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(videoRenderService.capabilities());
    }
}
