package com.dividendbot.news.service.engagement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dividendbot.news.domain.entity.ExternalMetricStatus;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class StructuredDataEngagementParser {
    private final ObjectMapper objectMapper;

    public ExternalEngagementMetrics parse(Document document) {
        Counts counts = new Counts();

        for (Element script : document.select("script[type=application/ld+json]")) {
            try {
                collectJson(objectMapper.readTree(script.data()), counts);
            } catch (Exception ignored) {
                // 한 개의 잘못된 JSON-LD 블록 때문에 다른 공개 구조화 데이터를 버리지 않습니다.
            }
        }

        for (Element counter : document.select("[itemprop=interactionStatistic]")) {
            String type = propertyValue(counter, "interactionType");
            Long count = parseCount(propertyValue(counter, "userInteractionCount"));
            counts.accept(type, count);
        }
        for (Element commentCount : document.select("[itemprop=commentCount]")) {
            counts.comments = max(counts.comments, parseCount(elementValue(commentCount)));
        }

        ExternalEngagementMetrics result = counts.toMetrics();
        return result.hasAnyCount()
                ? result
                : ExternalEngagementMetrics.unavailable(ExternalMetricStatus.NOT_SUPPORTED, "SCHEMA_ORG");
    }

    private void collectJson(JsonNode node, Counts counts) {
        if (node == null || node.isNull()) return;
        if (node.isObject()) {
            if (isSupportedContent(node.get("@type"))) {
                collectInteractionStatistics(node.get("interactionStatistic"), counts);
                counts.comments = max(counts.comments, readDirectCount(node, "commentCount"));
                counts.views = max(counts.views, readDirectCount(node, "viewCount"));
                counts.positive = max(counts.positive, readDirectCount(node, "likeCount"));
                counts.negative = max(counts.negative, readDirectCount(node, "dislikeCount"));
            }

            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) collectJson(fields.next().getValue(), counts);
        } else if (node.isArray()) {
            node.forEach(child -> collectJson(child, counts));
        }
    }

    private void collectInteractionStatistics(JsonNode node, Counts counts) {
        if (node == null || node.isNull()) return;
        if (node.isArray()) {
            node.forEach(child -> collectInteractionStatistics(child, counts));
            return;
        }
        if (!node.isObject()) return;

        JsonNode interactionType = node.get("interactionType");
        JsonNode interactionCount = node.get("userInteractionCount");
        if (interactionType != null && interactionCount != null) {
            counts.accept(readType(interactionType), parseCount(interactionCount.asText()));
        }
    }

    private boolean isSupportedContent(JsonNode typeNode) {
        if (typeNode == null || typeNode.isNull()) return false;
        if (typeNode.isArray()) {
            for (JsonNode child : typeNode) {
                if (isSupportedContent(child)) return true;
            }
            return false;
        }
        String type = typeNode.asText("").toLowerCase(Locale.ROOT);
        return type.contains("article")
                || type.contains("posting")
                || type.contains("videoobject")
                || type.contains("creativework");
    }

    private Long readDirectCount(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null ? null : parseCount(value.asText());
    }

    private String readType(JsonNode node) {
        if (node.isTextual()) return node.asText();
        if (node.isObject()) {
            JsonNode type = node.get("@type");
            if (type != null) return type.asText();
            JsonNode name = node.get("name");
            if (name != null) return name.asText();
        }
        return node.toString();
    }

    private String propertyValue(Element root, String property) {
        Element value = root.selectFirst("[itemprop=" + property + "]");
        return value == null ? "" : elementValue(value);
    }

    private String elementValue(Element element) {
        String content = element.attr("content");
        if (!content.isBlank()) return content;
        String href = element.attr("href");
        return href.isBlank() ? element.text() : href;
    }

    private Long parseCount(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            String normalized = raw.trim().replace(",", "").replaceAll("[^0-9]", "");
            return normalized.isBlank() ? null : Long.parseLong(normalized);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Long max(Long current, Long candidate) {
        if (candidate == null) return current;
        return current == null ? candidate : Math.max(current, candidate);
    }

    private static class Counts {
        private Long views;
        private Long comments;
        private Long positive;
        private Long negative;

        void accept(String rawType, Long count) {
            if (rawType == null || count == null) return;
            String type = rawType.toLowerCase(Locale.ROOT);
            if (type.contains("dislike")) negative = max(negative, count);
            else if (type.contains("like")) positive = max(positive, count);
            else if (type.contains("comment")) comments = max(comments, count);
            else if (type.contains("view") || type.contains("watch") || type.contains("read")) views = max(views, count);
        }

        ExternalEngagementMetrics toMetrics() {
            return new ExternalEngagementMetrics(views, comments, positive, negative, "SCHEMA_ORG", ExternalMetricStatus.AVAILABLE);
        }
    }
}
