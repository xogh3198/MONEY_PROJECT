package com.dividendbot.news.service.engagement;

import com.dividendbot.news.domain.entity.ExternalMetricStatus;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class RobotsPolicyService {
    static final String USER_AGENT = "InvestBoardBot";
    private static final Duration CACHE_TTL = Duration.ofHours(6);
    private final Map<String, CachedRobots> cache = new ConcurrentHashMap<>();

    public RobotsDecision evaluate(URI target) {
        String origin = target.getScheme().toLowerCase(Locale.ROOT) + "://" + target.getHost().toLowerCase(Locale.ROOT)
                + (target.getPort() == -1 ? "" : ":" + target.getPort());
        CachedRobots cached = cache.get(origin);
        if (cached == null || cached.expiresAt().isBefore(Instant.now())) {
            cached = fetch(origin);
            cache.put(origin, cached);
        }
        if (!cached.available()) {
            return new RobotsDecision(false, ExternalMetricStatus.ROBOTS_UNAVAILABLE);
        }

        String path = target.getRawPath() == null || target.getRawPath().isBlank() ? "/" : target.getRawPath();
        if (target.getRawQuery() != null) path += "?" + target.getRawQuery();
        boolean allowed = isAllowed(cached.body(), USER_AGENT, path);
        return new RobotsDecision(allowed, allowed ? ExternalMetricStatus.PENDING : ExternalMetricStatus.BLOCKED_BY_POLICY);
    }

    private CachedRobots fetch(String origin) {
        try {
            Connection.Response response = Jsoup.connect(origin + "/robots.txt")
                    .userAgent(USER_AGENT + "/1.0 (+https://investboard.cloud)")
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .maxBodySize(256 * 1024)
                    .timeout(5_000)
                    .execute();
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                return new CachedRobots(true, response.body(), Instant.now().plus(CACHE_TTL));
            }
            if (status == 401 || status == 403 || status == 429 || status >= 500) {
                return new CachedRobots(false, "", Instant.now().plus(Duration.ofHours(1)));
            }
            // RFC 9309: robots.txt가 존재하지 않는 일반 4xx 응답은 제한 없음으로 처리합니다.
            return new CachedRobots(true, "", Instant.now().plus(CACHE_TTL));
        } catch (Exception ignored) {
            return new CachedRobots(false, "", Instant.now().plus(Duration.ofHours(1)));
        }
    }

    static boolean isAllowed(String robotsBody, String userAgent, String path) {
        if (robotsBody == null || robotsBody.isBlank()) return true;
        List<Group> groups = parseGroups(robotsBody);
        String normalizedAgent = userAgent.toLowerCase(Locale.ROOT);
        List<Group> selected = groups.stream()
                .filter(group -> group.agents().stream().anyMatch(normalizedAgent::equals))
                .toList();
        if (selected.isEmpty()) {
            selected = groups.stream().filter(group -> group.agents().contains("*")).toList();
        }

        Rule best = null;
        for (Group group : selected) {
            for (Rule rule : group.rules()) {
                if (!rule.path().isBlank() && matches(rule.path(), path)) {
                    if (best == null || rule.specificity() > best.specificity()
                            || (rule.specificity() == best.specificity() && rule.allow())) {
                        best = rule;
                    }
                }
            }
        }
        return best == null || best.allow();
    }

    private static List<Group> parseGroups(String body) {
        List<Group> groups = new ArrayList<>();
        List<String> agents = new ArrayList<>();
        List<Rule> rules = new ArrayList<>();

        for (String rawLine : body.split("\\R")) {
            String line = rawLine.replaceFirst("#.*$", "").trim();
            if (line.isEmpty()) {
                if (!agents.isEmpty()) {
                    groups.add(new Group(List.copyOf(agents), List.copyOf(rules)));
                    agents.clear();
                    rules.clear();
                }
                continue;
            }
            int separator = line.indexOf(':');
            if (separator < 0) continue;
            String key = line.substring(0, separator).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(separator + 1).trim();

            if (key.equals("user-agent")) {
                if (!rules.isEmpty()) {
                    groups.add(new Group(List.copyOf(agents), List.copyOf(rules)));
                    agents.clear();
                    rules.clear();
                }
                agents.add(value.toLowerCase(Locale.ROOT));
            } else if ((key.equals("allow") || key.equals("disallow")) && !agents.isEmpty()) {
                if (!value.isBlank()) rules.add(new Rule(key.equals("allow"), value));
            }
        }
        if (!agents.isEmpty()) groups.add(new Group(List.copyOf(agents), List.copyOf(rules)));
        return groups;
    }

    private static boolean matches(String rulePath, String path) {
        boolean endAnchored = rulePath.endsWith("$");
        String raw = endAnchored ? rulePath.substring(0, rulePath.length() - 1) : rulePath;
        StringBuilder regexBuilder = new StringBuilder();
        for (String part : raw.split("\\*", -1)) {
            if (!regexBuilder.isEmpty()) regexBuilder.append(".*");
            regexBuilder.append(Pattern.quote(part));
        }
        String regex = regexBuilder.toString();
        return Pattern.compile("^" + regex + (endAnchored ? "$" : ".*")).matcher(path).matches();
    }

    public record RobotsDecision(boolean allowed, ExternalMetricStatus status) {}
    private record CachedRobots(boolean available, String body, Instant expiresAt) {}
    private record Group(List<String> agents, List<Rule> rules) {}
    private record Rule(boolean allow, String path) {
        int specificity() { return path.replace("*", "").replace("$", "").length(); }
    }
}
