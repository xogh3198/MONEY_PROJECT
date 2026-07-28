package com.dividendbot.news.service.video;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SpeechMarkCaptionBuilder {

    private final ObjectMapper objectMapper;

    public SpeechMarkCaptionBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<TimedCaption> fromPollyJsonLines(String jsonLines, double audioDurationSeconds) {
        List<WordMark> words = jsonLines.lines()
                .filter(line -> !line.isBlank())
                .map(this::parseWord)
                .filter(mark -> mark != null && !mark.value().isBlank())
                .toList();

        if (words.isEmpty()) return List.of();

        List<CaptionStart> starts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        double currentStart = words.get(0).timeMs() / 1000d;

        for (WordMark word : words) {
            String separator = current.isEmpty() ? "" : " ";
            boolean shouldBreak = !current.isEmpty() && current.length() + separator.length() + word.value().length() > 18;
            if (shouldBreak) {
                starts.add(new CaptionStart(currentStart, current.toString()));
                current.setLength(0);
                currentStart = word.timeMs() / 1000d;
            }
            if (!current.isEmpty()) current.append(' ');
            current.append(word.value());
            if (endsSentence(word.value()) && current.length() >= 8) {
                starts.add(new CaptionStart(currentStart, current.toString()));
                current.setLength(0);
            }
        }
        if (!current.isEmpty()) {
            starts.add(new CaptionStart(currentStart, current.toString()));
        }

        List<TimedCaption> captions = new ArrayList<>();
        for (int index = 0; index < starts.size(); index++) {
            CaptionStart start = starts.get(index);
            double end = index + 1 < starts.size()
                    ? Math.max(start.timeSeconds() + 0.2, starts.get(index + 1).timeSeconds())
                    : Math.max(start.timeSeconds() + 0.4, audioDurationSeconds);
            captions.add(new TimedCaption(start.timeSeconds(), end, start.text()));
        }
        return captions;
    }

    public List<TimedCaption> fallback(String narration, double audioDurationSeconds) {
        List<String> chunks = splitKorean(narration);
        if (chunks.isEmpty()) return List.of();
        double totalWeight = chunks.stream().mapToInt(String::length).sum();
        List<TimedCaption> captions = new ArrayList<>();
        double cursor = 0;
        for (int index = 0; index < chunks.size(); index++) {
            String chunk = chunks.get(index);
            double length = index == chunks.size() - 1
                    ? audioDurationSeconds - cursor
                    : audioDurationSeconds * (chunk.length() / totalWeight);
            double end = Math.max(cursor + 0.1, Math.min(audioDurationSeconds, cursor + length));
            captions.add(new TimedCaption(cursor, end, chunk));
            cursor = end;
        }
        return captions;
    }

    List<String> splitKorean(String narration) {
        if (narration == null || narration.isBlank()) return List.of();
        List<String> chunks = new ArrayList<>();
        String normalized = narration.replaceAll("\\s+", " ").trim();
        for (String sentence : normalized.split("(?<=[.!?。！？])\\s+")) {
            String remaining = sentence.trim();
            while (remaining.length() > 18) {
                int boundary = findBoundary(remaining, 18);
                chunks.add(remaining.substring(0, boundary).trim());
                remaining = remaining.substring(boundary).trim();
            }
            if (!remaining.isBlank()) chunks.add(remaining);
        }
        return chunks;
    }

    private WordMark parseWord(String line) {
        try {
            JsonNode node = objectMapper.readTree(line);
            if (!"word".equals(node.path("type").asText())) return null;
            return new WordMark(node.path("time").asLong(), node.path("value").asText(""));
        } catch (Exception ignored) {
            return null;
        }
    }

    private int findBoundary(String value, int target) {
        int upper = Math.min(value.length(), target);
        for (int index = upper; index >= Math.max(8, target - 6); index--) {
            char character = value.charAt(index - 1);
            if (Character.isWhitespace(character) || ",·:;".indexOf(character) >= 0) {
                return index;
            }
        }
        return upper;
    }

    private boolean endsSentence(String value) {
        return value.endsWith(".") || value.endsWith("?") || value.endsWith("!")
                || value.endsWith("。") || value.endsWith("？") || value.endsWith("！");
    }

    private record WordMark(long timeMs, String value) {
    }

    private record CaptionStart(double timeSeconds, String text) {
    }
}
