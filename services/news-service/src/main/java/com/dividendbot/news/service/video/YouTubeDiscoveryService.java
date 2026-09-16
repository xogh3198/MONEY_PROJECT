package com.dividendbot.news.service.video;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class YouTubeDiscoveryService {
    private static final Pattern QUERY = Pattern.compile("^[\\p{L}\\p{N}\\s._#-]{2,80}$");
    private final String apiKey;
    private final WebClient client = WebClient.builder().baseUrl("https://www.googleapis.com/youtube/v3").build();

    public YouTubeDiscoveryService(@Value("${external.youtube.api-key:}") String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean configured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public Map<String, Object> search(String query, String language, boolean strictAudio, boolean reusable, int minMinutes, int days) {
        if (!configured()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "YOUTUBE_API_KEY가 설정되지 않았습니다.");
        if (query == null || !QUERY.matcher(query.trim()).matches()) throw new IllegalArgumentException("검색어는 2~80자의 일반 문자여야 합니다.");
        if (!List.of("ko", "ja", "all").contains(language)) throw new IllegalArgumentException("지원하지 않는 언어 필터입니다.");
        if (minMinutes < 0 || minMinutes > 60 || days < 1 || days > 365) throw new IllegalArgumentException("기간 또는 영상 길이 범위를 확인해주세요.");

        try {
            JsonNode search = client.get().uri(builder -> {
                var uri = builder.path("/search")
                        .queryParam("part", "snippet")
                        .queryParam("type", "video")
                        .queryParam("maxResults", 40)
                        .queryParam("order", "viewCount")
                        .queryParam("q", query.trim())
                        .queryParam("publishedAfter", LocalDate.now(ZoneOffset.UTC).minusDays(days).atStartOfDay().toInstant(ZoneOffset.UTC))
                        .queryParam("key", apiKey);
                if (!"all".equals(language)) uri.queryParam("relevanceLanguage", language);
                if (reusable) uri.queryParam("videoLicense", "creativeCommon");
                return uri.build();
            }).retrieve().bodyToMono(JsonNode.class).block(Duration.ofSeconds(20));
            if (search == null || !search.path("items").isArray()) throw new IllegalStateException("YouTube 검색 결과를 읽지 못했습니다.");
            List<String> ids = new ArrayList<>();
            search.path("items").forEach(item -> {
                String id = item.path("id").path("videoId").asText("");
                if (id.matches("[A-Za-z0-9_-]{11}")) ids.add(id);
            });
            if (ids.isEmpty()) return Map.of("items", List.of(), "checkedAt", Instant.now().toString(), "searchCalls", 1, "detailQuotaUnits", 0);
            JsonNode details = client.get().uri(builder -> builder.path("/videos")
                    .queryParam("part", "snippet,contentDetails,status,statistics")
                    .queryParam("id", String.join(",", ids))
                    .queryParam("key", apiKey)
                    .build()).retrieve().bodyToMono(JsonNode.class).block(Duration.ofSeconds(20));
            if (details == null || !details.path("items").isArray()) throw new IllegalStateException("YouTube 영상 정보를 읽지 못했습니다.");
            List<Map<String, Object>> results = new ArrayList<>();
            details.path("items").forEach(video -> {
                JsonNode snippet = video.path("snippet");
                String audioLanguage = snippet.path("defaultAudioLanguage").asText("");
                String defaultLanguage = snippet.path("defaultLanguage").asText("");
                String knownLanguage = !audioLanguage.isBlank() ? audioLanguage : defaultLanguage;
                if (!"all".equals(language) && strictAudio && !audioLanguage.toLowerCase().startsWith(language)) return;
                if (!"all".equals(language) && !strictAudio && !knownLanguage.isBlank() && !knownLanguage.toLowerCase().startsWith(language)) return;
                String license = video.path("status").path("license").asText("");
                if (reusable && !"creativeCommon".equals(license)) return;
                long duration;
                try { duration = java.time.Duration.parse(video.path("contentDetails").path("duration").asText("PT0S")).toSeconds(); }
                catch (RuntimeException ignored) { return; }
                if (duration < (long) minMinutes * 60) return;
                String id = video.path("id").asText("");
                Map<String, Object> result = new HashMap<>();
                result.put("id", id);
                result.put("url", "https://www.youtube.com/watch?v=" + id);
                result.put("title", snippet.path("title").asText(""));
                result.put("channel", snippet.path("channelTitle").asText(""));
                result.put("thumbnail", snippet.path("thumbnails").path("medium").path("url").asText(""));
                result.put("publishedAt", snippet.path("publishedAt").asText(""));
                result.put("viewCount", video.path("statistics").path("viewCount").asLong(0));
                result.put("durationSeconds", duration);
                result.put("language", knownLanguage);
                result.put("license", license);
                results.add(result);
            });
            results.sort((a, b) -> Long.compare((long) b.get("viewCount"), (long) a.get("viewCount")));
            return Map.of("items", results, "checkedAt", Instant.now().toString(), "searchCalls", 1, "detailQuotaUnits", 1);
        } catch (ResponseStatusException error) {
            throw error;
        } catch (RuntimeException error) {
            // A WebClient exception can contain the request URL, including the API key.
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "YouTube API 조회에 실패했습니다. 키·할당량·API 활성화를 확인해주세요.");
        }
    }
}
