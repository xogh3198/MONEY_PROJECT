package com.dividendbot.news.controller;

import com.dividendbot.news.dto.VideoRenderJobResponse;
import com.dividendbot.news.dto.VideoRenderRequest;
import com.dividendbot.news.dto.ShortformClipRequest;
import com.dividendbot.news.dto.AiSceneGenerationJobResponse;
import com.dividendbot.news.dto.AiSceneGenerationRequest;
import com.dividendbot.news.dto.ReferenceVideoAnalysisRequest;
import com.dividendbot.news.dto.ReferenceVideoAnalysisResponse;
import com.dividendbot.news.service.video.AiSceneGenerationService;
import com.dividendbot.news.service.video.ApifyYouTubeReferenceService;
import com.dividendbot.news.service.video.VideoAssetStorage;
import com.dividendbot.news.service.video.VideoRenderAccessGuard;
import com.dividendbot.news.service.video.VideoRenderService;
import com.dividendbot.news.service.video.ShortformSourceStorage;
import com.dividendbot.news.service.video.YouTubeDiscoveryService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import com.dividendbot.news.service.video.VoiceTrack;

@RestController
@RequestMapping("/api/content-videos")
public class ContentVideoController {

    private final VideoRenderAccessGuard accessGuard;
    private final VideoAssetStorage assetStorage;
    private final VideoRenderService videoRenderService;
    private final AiSceneGenerationService aiSceneGenerationService;
    private final ApifyYouTubeReferenceService referenceVideoService;
    private final YouTubeDiscoveryService youtubeDiscovery;
    private final ShortformSourceStorage shortformSources;

    public ContentVideoController(
            VideoRenderAccessGuard accessGuard,
            VideoAssetStorage assetStorage,
            VideoRenderService videoRenderService,
            AiSceneGenerationService aiSceneGenerationService,
            ApifyYouTubeReferenceService referenceVideoService,
            YouTubeDiscoveryService youtubeDiscovery,
            ShortformSourceStorage shortformSources
    ) {
        this.accessGuard = accessGuard;
        this.assetStorage = assetStorage;
        this.videoRenderService = videoRenderService;
        this.aiSceneGenerationService = aiSceneGenerationService;
        this.referenceVideoService = referenceVideoService;
        this.youtubeDiscovery = youtubeDiscovery;
        this.shortformSources = shortformSources;
    }

    @GetMapping("/discovery/youtube")
    public ResponseEntity<Map<String, Object>> searchYouTube(
            @RequestHeader(value = "X-Video-Render-Key", required = false) String accessKey,
            @RequestParam String q,
            @RequestParam(defaultValue = "ko") String language,
            @RequestParam(defaultValue = "true") boolean strictAudio,
            @RequestParam(defaultValue = "false") boolean reusable,
            @RequestParam(defaultValue = "4") int minMinutes,
            @RequestParam(defaultValue = "30") int days
    ) {
        accessGuard.requireAuthorized(accessKey);
        try { return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(youtubeDiscovery.search(q, language, strictAudio, reusable, minMinutes, days)); }
        catch (IllegalArgumentException error) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error.getMessage(), error); }
    }

    @PostMapping("/shortform/sources")
    public ResponseEntity<Map<String, Object>> startShortformUpload(
            @RequestHeader(value = "X-Video-Render-Key", required = false) String accessKey,
            @RequestBody Map<String, Object> body
    ) {
        accessGuard.requireAuthorized(accessKey);
        try {
            long size = Long.parseLong(String.valueOf(body.get("size")));
            UUID id = shortformSources.start(size, String.valueOf(body.get("contentType")));
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("sourceId", id, "maxChunkBytes", ShortformSourceStorage.MAX_CHUNK_BYTES));
        } catch (IllegalArgumentException error) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error.getMessage(), error); }
    }

    @PostMapping(value = "/shortform/sources/{sourceId}/chunks", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Map<String, Object>> uploadShortformChunk(
            @RequestHeader(value = "X-Video-Render-Key", required = false) String accessKey,
            @PathVariable UUID sourceId,
            @RequestHeader("X-Upload-Offset") long offset,
            @RequestBody byte[] chunk
    ) {
        accessGuard.requireAuthorized(accessKey);
        try { return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(Map.of("uploadedBytes", shortformSources.append(sourceId, offset, chunk))); }
        catch (IllegalArgumentException error) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error.getMessage(), error); }
    }

    @PostMapping("/shortform/sources/{sourceId}/complete")
    public ResponseEntity<Map<String, Object>> completeShortformUpload(
            @RequestHeader(value = "X-Video-Render-Key", required = false) String accessKey,
            @PathVariable UUID sourceId
    ) {
        accessGuard.requireAuthorized(accessKey);
        try { return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(Map.of("sourceId", sourceId, "durationSeconds", shortformSources.complete(sourceId).durationSeconds())); }
        catch (IllegalArgumentException error) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error.getMessage(), error); }
    }

    @PostMapping("/shortform/render")
    public ResponseEntity<VideoRenderJobResponse> renderShortform(
            @RequestHeader(value = "X-Video-Render-Key", required = false) String accessKey,
            @RequestBody ShortformClipRequest request
    ) {
        accessGuard.requireAuthorized(accessKey);
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(videoRenderService.submitClip(request));
    }

    @PostMapping("/reference-analysis")
    public ResponseEntity<ReferenceVideoAnalysisResponse> analyzeReference(
            @RequestHeader(value = "X-Video-Render-Key", required = false) String accessKey,
            @Valid @RequestBody ReferenceVideoAnalysisRequest request
    ) {
        accessGuard.requireAuthorized(accessKey);
        try {
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .body(referenceVideoService.analyze(request));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    @PostMapping("/ai-assets")
    public ResponseEntity<AiSceneGenerationJobResponse> generateAiAsset(
            @RequestHeader(value = "X-Video-Render-Key", required = false) String accessKey,
            @Valid @RequestBody AiSceneGenerationRequest request
    ) {
        accessGuard.requireAuthorized(accessKey);
        return ResponseEntity.accepted()
                .cacheControl(CacheControl.noStore())
                .body(aiSceneGenerationService.submit(request));
    }

    @GetMapping("/ai-assets/{jobId}")
    public ResponseEntity<AiSceneGenerationJobResponse> getAiAsset(
            @RequestHeader(value = "X-Video-Render-Key", required = false) String accessKey,
            @PathVariable UUID jobId
    ) {
        accessGuard.requireAuthorized(accessKey);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(aiSceneGenerationService.get(jobId));
    }

    @GetMapping("/ai-assets/{jobId}/file")
    public ResponseEntity<Resource> aiAssetFile(
            @RequestHeader(value = "X-Video-Render-Key", required = false) String accessKey,
            @PathVariable UUID jobId
    ) {
        accessGuard.requireAuthorized(accessKey);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType("video/mp4"))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + aiSceneGenerationService.fileName(jobId) + "\""
                )
                .body(aiSceneGenerationService.getFile(jobId));
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
        Map<String, Object> capabilities = new HashMap<>(videoRenderService.capabilities());
        capabilities.put("higgsfieldConfigured", aiSceneGenerationService.configured());
        capabilities.put("aiSceneProvider", aiSceneGenerationService.providerName());
        capabilities.put("apifyReferenceConfigured", referenceVideoService.configured());
        capabilities.put("referenceAnalysisProvider", "APIFY");
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(capabilities);
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
