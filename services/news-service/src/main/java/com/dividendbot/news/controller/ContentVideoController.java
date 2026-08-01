package com.dividendbot.news.controller;

import com.dividendbot.news.dto.VideoRenderJobResponse;
import com.dividendbot.news.dto.VideoRenderRequest;
import com.dividendbot.news.service.video.VideoAssetStorage;
import com.dividendbot.news.service.video.VideoRenderAccessGuard;
import com.dividendbot.news.service.video.VideoRenderService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import com.dividendbot.news.service.video.VoiceTrack;

@RestController
@RequestMapping("/api/content-videos")
public class ContentVideoController {

    private final VideoRenderAccessGuard accessGuard;
    private final VideoAssetStorage assetStorage;
    private final VideoRenderService videoRenderService;

    public ContentVideoController(
            VideoRenderAccessGuard accessGuard,
            VideoAssetStorage assetStorage,
            VideoRenderService videoRenderService
    ) {
        this.accessGuard = accessGuard;
        this.assetStorage = assetStorage;
        this.videoRenderService = videoRenderService;
    }

    @PostMapping(value = "/assets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadAsset(
            @RequestHeader(value = "X-Video-Render-Key", required = false) String accessKey,
            @RequestPart("file") MultipartFile file
    ) {
        accessGuard.requireAuthorized(accessKey);
        VideoAssetStorage.StoredVideoAsset stored;
        try {
            stored = assetStorage.store(file);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of(
                        "assetRef", stored.reference(),
                        "mediaKind", stored.mediaKind().name(),
                        "contentType", stored.contentType(),
                        "fileName", file.getOriginalFilename() == null ? "scene-asset" : file.getOriginalFilename()
                ));
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

    @PostMapping("/voice-preview")
    public ResponseEntity<Resource> voicePreview(
            @RequestHeader(value = "X-Video-Render-Key", required = false) String accessKey,
            @RequestBody Map<String, String> body
    ) {
        accessGuard.requireAuthorized(accessKey);
        String provider = body.getOrDefault("provider", "POLLY");
        String voiceIdParam = body.getOrDefault("voiceId", "");
        String text = body.getOrDefault("text", "안녕하세요, AI 음성 미리듣기입니다.");
        String style = body.getOrDefault("voiceStyle", "NATURAL");
        try {
            Path tempDir = Files.createTempDirectory("voice-preview-");
            VoiceTrack track = videoRenderService.previewVoice(provider, voiceIdParam, text, style, tempDir);
            Resource resource = new FileSystemResource(track.audioFile());
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .contentType(MediaType.parseMediaType("audio/mpeg"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"preview.mp3\"")
                    .body(resource);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "음성 미리듣기 생성에 실패했습니다: " + e.getMessage());
        }
    }
}
