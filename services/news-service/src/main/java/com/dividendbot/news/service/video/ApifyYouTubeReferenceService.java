package com.dividendbot.news.service.video;

import com.dividendbot.news.dto.ReferenceVideoAnalysisRequest;
import com.dividendbot.news.dto.ReferenceVideoAnalysisResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ApifyYouTubeReferenceService {

    private static final Pattern VIDEO_ID = Pattern.compile("^[A-Za-z0-9_-]{11}$");
    private static final Pattern ACTOR_ID = Pattern.compile("^[A-Za-z0-9._~-]{3,120}$");
    private static final String APIFY_API_BASE = "https://api.apify.com";

    private final ObjectMapper objectMapper;
    private final String token;
    private final String actorId;
    private final boolean enabled;
    private final HttpClient httpClient;

    public ApifyYouTubeReferenceService(
            ObjectMapper objectMapper,
            @Value("${video.reference.apify-token:}") String token,
            @Value("${video.reference.apify-actor-id:apihq~youtube-transcript-scraper}") String actorId,
            @Value("${video.reference.apify-enabled:false}") boolean enabled
    ) {
        this.objectMapper = objectMapper;
        this.token = token == null ? "" : token.trim();
        this.actorId = ACTOR_ID.matcher(actorId == null ? "" : actorId.trim()).matches()
                ? actorId.trim()
                : "apihq~youtube-transcript-scraper";
        this.enabled = enabled;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public boolean configured() {
        return enabled && !token.isBlank();
    }

    public ReferenceVideoAnalysisResponse analyze(ReferenceVideoAnalysisRequest request) {
        String sourceUrl = request.referenceVideoUrl().trim();
        String videoId = extractVideoId(sourceUrl)
                .orElseThrow(() -> new IllegalArgumentException("지원하는 YouTube 영상 URL이 아닙니다."));
        String requestedStyle = normalizeStyle(request.stylePrompt());

        if (!configured()) {
            return unavailable(
                    sourceUrl,
                    "NOT_CONFIGURED",
                    requestedStyle,
                    "Apify 분석이 꺼져 있습니다. 스타일 프롬프트만 사용하며 원문은 복사하지 않습니다."
            );
        }

        try {
            URI endpoint = URI.create(
                    APIFY_API_BASE + "/v2/acts/" + actorId
                            + "/run-sync-get-dataset-items?timeout=45&memory=256"
            );
            String body = objectMapper.writeValueAsString(java.util.Map.of(
                    "videoId", videoId,
                    "metadata", true
            ));
            HttpRequest httpRequest = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(55))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return unavailable(
                        sourceUrl,
                        "FAILED",
                        requestedStyle,
                        "참고 영상 분석을 완료하지 못했습니다. HTTP " + response.statusCode()
                );
            }

            JsonNode rows = objectMapper.readTree(response.body());
            JsonNode row = rows.isArray() && !rows.isEmpty() ? rows.get(0) : null;
            if (row == null || !row.path("success").asBoolean(false)) {
                String code = row == null ? "EMPTY_RESULT" : row.path("code").asText("UNAVAILABLE");
                return unavailable(
                        sourceUrl,
                        "UNAVAILABLE",
                        requestedStyle,
                        "공개 자막을 가져오지 못했습니다: " + safeCode(code)
                );
            }

            JsonNode transcript = row.path("transcript");
            JsonNode metadata = row.path("metadata");
            int durationSeconds = Math.max(0, metadata.path("duration_seconds").asInt(0));
            int cueCount = transcript.isArray() ? transcript.size() : 0;
            double totalCueDuration = 0;
            int totalCharacters = 0;
            int openingCueCount = 0;

            if (transcript.isArray()) {
                for (JsonNode cue : transcript) {
                    double start = Math.max(0, cue.path("start").asDouble(0));
                    double cueDuration = Math.max(0, cue.path("duration").asDouble(0));
                    totalCueDuration += cueDuration;
                    totalCharacters += cue.path("text").asText("").codePointCount(0, cue.path("text").asText("").length());
                    if (start < 5) openingCueCount++;
                }
            }

            double averageCueSeconds = cueCount == 0 ? 0 : totalCueDuration / cueCount;
            double speechDensity = durationSeconds == 0 ? 0 : totalCharacters / (double) durationSeconds;
            String styleSummary = buildStyleSummary(
                    requestedStyle,
                    durationSeconds,
                    cueCount,
                    averageCueSeconds,
                    openingCueCount,
                    speechDensity
            );
            return new ReferenceVideoAnalysisResponse(
                    "APIFY",
                    "ANALYZED",
                    sourceUrl,
                    metadata.path("title").asText(""),
                    metadata.path("author_name").asText(""),
                    durationSeconds,
                    cueCount,
                    round(averageCueSeconds),
                    openingCueCount,
                    round(speechDensity),
                    styleSummary,
                    "자막 원문과 영상은 저장하거나 재사용하지 않고 길이·밀도 지표만 사용합니다."
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return unavailable(sourceUrl, "FAILED", requestedStyle, "참고 영상 분석이 중단되었습니다.");
        } catch (Exception exception) {
            return unavailable(sourceUrl, "FAILED", requestedStyle, "참고 영상 분석을 완료하지 못했습니다.");
        }
    }

    static Optional<String> extractVideoId(String rawUrl) {
        try {
            URI uri = URI.create(rawUrl.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme())) {
                return Optional.empty();
            }
            String host = Optional.ofNullable(uri.getHost()).orElse("").toLowerCase(Locale.ROOT);
            String candidate = "";
            if (host.equals("youtu.be")) {
                candidate = firstPathSegment(uri.getPath());
            } else if (host.equals("youtube.com") || host.equals("www.youtube.com") || host.equals("m.youtube.com")) {
                String path = Optional.ofNullable(uri.getPath()).orElse("");
                if (path.equals("/watch")) {
                    candidate = queryParam(uri.getRawQuery(), "v").orElse("");
                } else if (path.startsWith("/shorts/") || path.startsWith("/embed/")) {
                    String[] parts = path.split("/");
                    candidate = parts.length > 2 ? parts[2] : "";
                }
            }
            return VIDEO_ID.matcher(candidate).matches() ? Optional.of(candidate) : Optional.empty();
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static String firstPathSegment(String path) {
        if (path == null) return "";
        for (String part : path.split("/")) {
            if (!part.isBlank()) return part;
        }
        return "";
    }

    private static Optional<String> queryParam(String query, String key) {
        if (query == null || query.isBlank()) return Optional.empty();
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && URLDecoder.decode(parts[0], StandardCharsets.UTF_8).equals(key)) {
                return Optional.of(URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
            }
        }
        return Optional.empty();
    }

    private static String normalizeStyle(String value) {
        if (value == null || value.isBlank()) return "사람이 편집한 듯 자연스럽고 정보 중심인 숏폼";
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.length() > 800 ? normalized.substring(0, 800) : normalized;
    }

    private static ReferenceVideoAnalysisResponse unavailable(
            String sourceUrl,
            String status,
            String requestedStyle,
            String note
    ) {
        return new ReferenceVideoAnalysisResponse(
                "APIFY",
                status,
                sourceUrl,
                "",
                "",
                null,
                null,
                null,
                null,
                null,
                requestedStyle + ". 참고 영상의 표현·대본·장면은 복제하지 않습니다.",
                note
        );
    }

    private static String buildStyleSummary(
            String requestedStyle,
            int durationSeconds,
            int cueCount,
            double averageCueSeconds,
            int openingCueCount,
            double speechDensity
    ) {
        String pace = averageCueSeconds > 0 && averageCueSeconds <= 2.2 ? "빠른 자막 전환" : "여유 있는 자막 전환";
        String density = speechDensity >= 7 ? "정보 밀도가 높은 전달" : "짧고 여백 있는 전달";
        return requestedStyle + ". 참고 구조: " + durationSeconds + "초, 자막 " + cueCount + "개, 평균 "
                + round(averageCueSeconds) + "초, 첫 5초 자막 " + openingCueCount + "개, " + pace + ", " + density
                + ". 원문의 문장·영상·음성은 복제하지 않습니다.";
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static String safeCode(String value) {
        Matcher matcher = Pattern.compile("[A-Z0-9_]{1,40}").matcher(value == null ? "" : value.toUpperCase(Locale.ROOT));
        return matcher.matches() ? matcher.group() : "UNAVAILABLE";
    }
}
