package com.dividendbot.news.service.video;

import com.dividendbot.news.dto.AiSceneGenerationRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class HiggsfieldSceneProvider implements AiSceneProvider {

    private static final String BASE_URL = "https://platform.higgsfield.ai";
    private static final int MAX_VIDEO_BYTES = 25 * 1024 * 1024;
    private static final Pattern MODEL_ID = Pattern.compile("^[a-z0-9._/-]{3,160}$");

    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String apiSecret;
    private final boolean enabled;
    private final String imageModel;
    private final String videoModel;
    private final String imageResolution;
    private final Duration maxPollTime;
    private final HttpClient httpClient;

    public HiggsfieldSceneProvider(
            ObjectMapper objectMapper,
            @Value("${video.assets.higgsfield.api-key:}") String apiKey,
            @Value("${video.assets.higgsfield.api-secret:}") String apiSecret,
            @Value("${video.assets.higgsfield.enabled:false}") boolean enabled,
            @Value("${video.assets.higgsfield.image-model:higgsfield-ai/soul/standard}") String imageModel,
            @Value("${video.assets.higgsfield.video-model:higgsfield-ai/dop/standard}") String videoModel,
            @Value("${video.assets.higgsfield.image-resolution:2K}") String imageResolution,
            @Value("${video.assets.higgsfield.max-poll-seconds:480}") long maxPollSeconds
    ) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.apiSecret = apiSecret == null ? "" : apiSecret.trim();
        this.enabled = enabled;
        this.imageModel = validModel(imageModel, "higgsfield-ai/soul/standard");
        this.videoModel = validModel(videoModel, "higgsfield-ai/dop/standard");
        this.imageResolution = normalizeResolution(imageResolution);
        this.maxPollTime = Duration.ofSeconds(Math.max(60, Math.min(maxPollSeconds, 900)));
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public boolean configured() {
        return enabled && !apiKey.isBlank() && !apiSecret.isBlank();
    }

    @Override
    public String name() {
        return "HIGGSFIELD";
    }

    @Override
    public GeneratedSceneVideo generate(AiSceneGenerationRequest request) {
        if (!configured()) {
            throw new IllegalStateException("Higgsfield API가 활성화되지 않았습니다.");
        }

        String visualPrompt = buildVisualPrompt(request);
        JsonNode imageResult = submitAndWait(imageModel, Map.of(
                "prompt", visualPrompt,
                "aspect_ratio", "9:16",
                "resolution", imageResolution,
                "camera_fixed", false
        ));
        String imageUrl = mediaUrl(imageResult, "images")
                .orElseThrow(() -> new IllegalStateException("Higgsfield 이미지 URL이 없습니다."));
        assertPublicHttps(URI.create(imageUrl));

        JsonNode videoResult = submitAndWait(videoModel, Map.of(
                "image_url", imageUrl,
                "prompt", buildMotionPrompt(request),
                "duration", 5
        ));
        String videoUrl = mediaUrl(videoResult, "video")
                .orElseThrow(() -> new IllegalStateException("Higgsfield 영상 URL이 없습니다."));
        DownloadedVideo downloaded = downloadVideo(URI.create(videoUrl));

        return new GeneratedSceneVideo(
                downloaded.data(),
                downloaded.contentType(),
                "higgsfield-scene-" + request.sceneOrder() + ".mp4",
                "Higgsfield AI 생성 장면 · 게시 전 사실·상표·인물·사용권 확인"
        );
    }

    private JsonNode submitAndWait(String modelId, Map<String, Object> payload) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + "/" + modelId))
                    .timeout(Duration.ofSeconds(45))
                    .header("Authorization", authorization())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(payload),
                            StandardCharsets.UTF_8
                    ))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Higgsfield 생성 요청이 거절되었습니다. HTTP " + response.statusCode());
            }
            JsonNode state = objectMapper.readTree(response.body());
            String requestId = state.path("request_id").asText("");
            UUID.fromString(requestId);
            Instant deadline = Instant.now().plus(maxPollTime);

            while (!"completed".equalsIgnoreCase(state.path("status").asText(""))) {
                String status = state.path("status").asText("").toLowerCase(Locale.ROOT);
                if (List.of("failed", "nsfw", "cancelled").contains(status)) {
                    throw new IllegalStateException("Higgsfield 생성이 완료되지 않았습니다: " + status);
                }
                if (Instant.now().isAfter(deadline)) {
                    throw new IllegalStateException("Higgsfield 생성 시간이 제한을 초과했습니다.");
                }
                Thread.sleep(3_000);
                state = requestStatus(requestId);
            }
            return state;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Higgsfield 생성이 중단되었습니다.", exception);
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Higgsfield 장면 생성에 실패했습니다.", exception);
        }
    }

    private JsonNode requestStatus(String requestId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(BASE_URL + "/requests/" + requestId + "/status")
                )
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", authorization())
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Higgsfield 상태 조회에 실패했습니다. HTTP " + response.statusCode());
        }
        return objectMapper.readTree(response.body());
    }

    private DownloadedVideo downloadVideo(URI initialUri) {
        URI uri = initialUri;
        for (int redirect = 0; redirect <= 3; redirect++) {
            try {
                assertPublicHttps(uri);
                HttpRequest request = HttpRequest.newBuilder(uri)
                        .timeout(Duration.ofSeconds(60))
                        .header("Accept", "video/mp4,video/*;q=0.9,application/octet-stream;q=0.5")
                        .header("User-Agent", "InvestBoardVideo/1.0")
                        .GET()
                        .build();
                HttpResponse<InputStream> response = httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofInputStream()
                );
                if (response.statusCode() >= 300 && response.statusCode() < 400) {
                    String location = response.headers().firstValue("location")
                            .orElseThrow(() -> new IllegalStateException("영상 리디렉션 위치가 없습니다."));
                    uri = uri.resolve(location);
                    continue;
                }
                if (response.statusCode() != 200) {
                    throw new IllegalStateException("생성 영상을 내려받지 못했습니다. HTTP " + response.statusCode());
                }
                String contentType = response.headers().firstValue("content-type")
                        .orElse("video/mp4")
                        .split(";", 2)[0]
                        .trim()
                        .toLowerCase(Locale.ROOT);
                if (!"video/mp4".equals(contentType) && !"application/octet-stream".equals(contentType)) {
                    throw new IllegalStateException("생성 결과가 MP4 영상 형식이 아닙니다.");
                }
                try (InputStream input = response.body()) {
                    byte[] bytes = input.readNBytes(MAX_VIDEO_BYTES + 1);
                    if (bytes.length == 0 || bytes.length > MAX_VIDEO_BYTES) {
                        throw new IllegalStateException("생성 영상은 25MB 이하여야 합니다.");
                    }
                    return new DownloadedVideo(bytes, "video/mp4");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("생성 영상 다운로드가 중단되었습니다.", exception);
            } catch (IllegalStateException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new IllegalStateException("생성 영상을 안전하게 저장하지 못했습니다.", exception);
            }
        }
        throw new IllegalStateException("생성 영상 리디렉션이 너무 많습니다.");
    }

    private String authorization() {
        return "Key " + apiKey + ":" + apiSecret;
    }

    private static Optional<String> mediaUrl(JsonNode response, String type) {
        if ("images".equals(type)) {
            JsonNode images = response.path("images");
            if (images.isArray() && !images.isEmpty()) {
                String url = images.get(0).path("url").asText("");
                return url.isBlank() ? Optional.empty() : Optional.of(url);
            }
            return Optional.empty();
        }
        String url = response.path("video").path("url").asText("");
        return url.isBlank() ? Optional.empty() : Optional.of(url);
    }

    private static String buildVisualPrompt(AiSceneGenerationRequest request) {
        String terms = request.visualSearchTerms() == null ? "" : String.join(", ", request.visualSearchTerms());
        return "Vertical 9:16 commercial b-roll for a Korean short-form explainer. Scene: "
                + normalize(request.visualDirection(), 240) + ". Subject keywords: " + normalize(terms, 240)
                + ". Intended caption meaning: " + normalize(request.onScreenText(), 100)
                + ". Style: " + normalize(request.stylePrompt(), 500)
                + ". Natural documentary lighting, believable details, clear single subject, mobile-safe composition. "
                + "No readable text, no logos, no copyrighted characters, no identifiable public figures, no misleading UI, no watermark.";
    }

    private static String buildMotionPrompt(AiSceneGenerationRequest request) {
        return "Subtle realistic motion for a 5-second vertical social video. Slow handheld push-in or gentle parallax, "
                + "stable subject, natural movement, no scene cut, no text, no logo, no morphing. "
                + normalize(request.visualDirection(), 240);
    }

    private static String normalize(String value, int maxLength) {
        if (value == null || value.isBlank()) return "clean informative commercial scene";
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private static String validModel(String value, String fallback) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return MODEL_ID.matcher(normalized).matches() ? normalized : fallback;
    }

    private static String normalizeResolution(String value) {
        String normalized = value == null ? "2K" : value.trim().toUpperCase(Locale.ROOT);
        return List.of("1K", "2K", "4K").contains(normalized) ? normalized : "2K";
    }

    private static void assertPublicHttps(URI uri) {
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalStateException("Higgsfield 결과 URL은 공개 HTTPS 주소여야 합니다.");
        }
        try {
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                byte[] raw = address.getAddress();
                boolean uniqueLocalV6 = raw.length == 16 && (raw[0] & 0xfe) == 0xfc;
                if (address.isAnyLocalAddress()
                        || address.isLoopbackAddress()
                        || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress()
                        || address.isMulticastAddress()
                        || uniqueLocalV6) {
                    throw new IllegalStateException("비공개 네트워크의 영상 URL은 사용할 수 없습니다.");
                }
            }
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Higgsfield 결과 호스트를 확인하지 못했습니다.", exception);
        }
    }

    private record DownloadedVideo(byte[] data, String contentType) {
    }
}
