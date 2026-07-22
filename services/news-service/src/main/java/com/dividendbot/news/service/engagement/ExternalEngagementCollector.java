package com.dividendbot.news.service.engagement;

import com.dividendbot.news.domain.entity.ExternalMetricStatus;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExternalEngagementCollector {
    private static final int MAX_REDIRECTS = 3;

    private final PublicUrlGuard publicUrlGuard;
    private final RobotsPolicyService robotsPolicyService;
    private final StructuredDataEngagementParser structuredDataParser;

    @Value("${external.youtube.api-key:}")
    private String youtubeApiKey;

    public ExternalEngagementMetrics collect(String rawUrl) {
        Optional<URI> parsed = publicUrlGuard.parse(rawUrl);
        if (parsed.isEmpty()) {
            return ExternalEngagementMetrics.unavailable(ExternalMetricStatus.INVALID_URL, "NONE");
        }
        URI uri = parsed.get();
        String host = uri.getHost().toLowerCase(Locale.ROOT);

        if (isNaverNewsHost(host)) {
            return ExternalEngagementMetrics.unavailable(ExternalMetricStatus.BLOCKED_BY_POLICY, "NAVER_NEWS");
        }
        Optional<String> youtubeId = extractYoutubeId(uri);
        if (youtubeId.isPresent()) return collectYoutube(youtubeId.get());

        return collectStructuredData(uri, 0);
    }

    private ExternalEngagementMetrics collectStructuredData(URI uri, int redirects) {
        if (redirects > MAX_REDIRECTS) {
            return ExternalEngagementMetrics.unavailable(ExternalMetricStatus.FETCH_ERROR, "SCHEMA_ORG");
        }

        RobotsPolicyService.RobotsDecision robots = robotsPolicyService.evaluate(uri);
        if (!robots.allowed()) {
            return ExternalEngagementMetrics.unavailable(robots.status(), "SCHEMA_ORG");
        }

        try {
            Connection.Response response = Jsoup.connect(uri.toString())
                    .userAgent(RobotsPolicyService.USER_AGENT + "/1.0 (+https://investboard.cloud)")
                    .header("Accept", "text/html,application/xhtml+xml")
                    .followRedirects(false)
                    .ignoreHttpErrors(true)
                    .maxBodySize(2 * 1024 * 1024)
                    .timeout(8_000)
                    .execute();

            int status = response.statusCode();
            if (status >= 300 && status < 400) {
                String location = response.header("Location");
                if (location == null || location.isBlank()) {
                    return ExternalEngagementMetrics.unavailable(ExternalMetricStatus.FETCH_ERROR, "SCHEMA_ORG");
                }
                URI redirected = uri.resolve(location);
                Optional<URI> safeRedirect = publicUrlGuard.parse(redirected.toString());
                return safeRedirect.map(value -> collectStructuredData(value, redirects + 1))
                        .orElseGet(() -> ExternalEngagementMetrics.unavailable(ExternalMetricStatus.INVALID_URL, "SCHEMA_ORG"));
            }
            if (status < 200 || status >= 300) {
                return ExternalEngagementMetrics.unavailable(ExternalMetricStatus.FETCH_ERROR, "SCHEMA_ORG");
            }
            String contentType = response.contentType();
            if (contentType != null && !contentType.toLowerCase(Locale.ROOT).contains("html")) {
                return ExternalEngagementMetrics.unavailable(ExternalMetricStatus.NOT_SUPPORTED, "SCHEMA_ORG");
            }
            Document document = response.parse();
            return structuredDataParser.parse(document);
        } catch (Exception ignored) {
            return ExternalEngagementMetrics.unavailable(ExternalMetricStatus.FETCH_ERROR, "SCHEMA_ORG");
        }
    }

    private ExternalEngagementMetrics collectYoutube(String videoId) {
        if (youtubeApiKey == null || youtubeApiKey.isBlank()) {
            return ExternalEngagementMetrics.unavailable(ExternalMetricStatus.NOT_CONFIGURED, "YOUTUBE_API");
        }
        try {
            JsonNode response = WebClient.builder()
                    .baseUrl("https://www.googleapis.com")
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/youtube/v3/videos")
                            .queryParam("part", "statistics")
                            .queryParam("id", videoId)
                            .queryParam("key", youtubeApiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(8));
            JsonNode statistics = response == null ? null : response.path("items").path(0).path("statistics");
            if (statistics == null || statistics.isMissingNode()) {
                return ExternalEngagementMetrics.unavailable(ExternalMetricStatus.NOT_SUPPORTED, "YOUTUBE_API");
            }
            return new ExternalEngagementMetrics(
                    count(statistics, "viewCount"),
                    count(statistics, "commentCount"),
                    count(statistics, "likeCount"),
                    null,
                    "YOUTUBE_API",
                    ExternalMetricStatus.AVAILABLE
            );
        } catch (Exception ignored) {
            return ExternalEngagementMetrics.unavailable(ExternalMetricStatus.FETCH_ERROR, "YOUTUBE_API");
        }
    }

    private Long count(JsonNode node, String field) {
        try {
            JsonNode value = node.get(field);
            return value == null ? null : Long.parseLong(value.asText());
        } catch (Exception ignored) {
            return null;
        }
    }

    private Optional<String> extractYoutubeId(URI uri) {
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        String path = uri.getPath() == null ? "" : uri.getPath();
        if (host.equals("youtu.be") || host.endsWith(".youtu.be")) {
            return firstPathSegment(path);
        }
        if (!host.equals("youtube.com") && !host.endsWith(".youtube.com")) return Optional.empty();

        if (path.startsWith("/shorts/") || path.startsWith("/embed/")) {
            String[] parts = path.split("/");
            return parts.length > 2 ? sanitizeYoutubeId(parts[2]) : Optional.empty();
        }
        if (uri.getRawQuery() != null) {
            for (String pair : uri.getRawQuery().split("&")) {
                String[] parts = pair.split("=", 2);
                if (parts.length == 2 && parts[0].equals("v")) return sanitizeYoutubeId(parts[1]);
            }
        }
        return Optional.empty();
    }

    private Optional<String> firstPathSegment(String path) {
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        int slash = normalized.indexOf('/');
        return sanitizeYoutubeId(slash >= 0 ? normalized.substring(0, slash) : normalized);
    }

    private Optional<String> sanitizeYoutubeId(String value) {
        return value != null && value.matches("[A-Za-z0-9_-]{6,20}") ? Optional.of(value) : Optional.empty();
    }

    private boolean isNaverNewsHost(String host) {
        return host.equals("news.naver.com") || host.equals("n.news.naver.com") || host.equals("apis.naver.com");
    }
}
