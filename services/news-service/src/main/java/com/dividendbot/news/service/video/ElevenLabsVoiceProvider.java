package com.dividendbot.news.service.video;

import com.dividendbot.news.domain.entity.VideoVoiceStyle;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class ElevenLabsVoiceProvider implements VoiceProvider {

    private static final Pattern SAFE_VOICE_ID = Pattern.compile("^[A-Za-z0-9_-]{6,80}$");

    private final ObjectMapper objectMapper;
    private final SpeechMarkCaptionBuilder captionBuilder;
    private final MediaProbe mediaProbe;
    private final String apiKey;
    private final String voiceId;
    private final String modelId;
    private final HttpClient httpClient;

    public ElevenLabsVoiceProvider(
            ObjectMapper objectMapper,
            SpeechMarkCaptionBuilder captionBuilder,
            MediaProbe mediaProbe,
            @Value("${video.voice.elevenlabs-api-key:}") String apiKey,
            @Value("${video.voice.elevenlabs-voice-id:}") String voiceId,
            @Value("${video.voice.elevenlabs-model-id:eleven_v3}") String modelId
    ) {
        this.objectMapper = objectMapper;
        this.captionBuilder = captionBuilder;
        this.mediaProbe = mediaProbe;
        this.apiKey = apiKey;
        this.voiceId = voiceId;
        this.modelId = modelId;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String name() {
        return "ELEVENLABS";
    }

    @Override
    public boolean configured() {
        return apiKey != null && !apiKey.isBlank()
                && voiceId != null && SAFE_VOICE_ID.matcher(voiceId).matches();
    }

    @Override
    public String displayName() {
        return "ElevenLabs";
    }

    @Override
    public String tier() {
        return "PREMIUM";
    }

    @Override
    public List<Map<String, Object>> availableVoices() {
        if (!configured()) return List.of();
        return List.of(
                Map.of("id", voiceId, "name", "기본 음성", "gender", "UNKNOWN",
                        "description", "설정된 기본 ElevenLabs 음성",
                        "styles", List.of("NATURAL", "WHISPER", "SNARKY"))
        );
    }

    @Override
    public VoiceTrack synthesize(
            String narration,
            Path outputDirectory,
            String fileStem,
            VideoVoiceStyle voiceStyle
    ) {
        return synthesize(narration, outputDirectory, fileStem, voiceStyle, null);
    }

    @Override
    public VoiceTrack synthesize(
            String narration,
            Path outputDirectory,
            String fileStem,
            VideoVoiceStyle voiceStyle,
            String requestedVoiceId
    ) {
        String effectiveVoiceId = (requestedVoiceId != null && !requestedVoiceId.isBlank())
                ? requestedVoiceId.trim() : this.voiceId;
        if (!configured()) throw new IllegalStateException("ElevenLabs API 설정이 비어 있습니다.");
        try {
            Files.createDirectories(outputDirectory);
            Path audioFile = outputDirectory.resolve(fileStem + ".mp3");
            VideoVoiceStyle normalizedStyle = voiceStyle == null ? VideoVoiceStyle.NATURAL : voiceStyle;

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("text", styledText(narration, normalizedStyle));
            payload.put("model_id", modelId);
            payload.put("language_code", "ko");

            URI uri = URI.create(
                    "https://api.elevenlabs.io/v1/text-to-speech/"
                            + effectiveVoiceId
                            + "/with-timestamps?output_format=mp3_44100_128"
            );
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(90))
                    .header("Content-Type", "application/json")
                    .header("xi-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(payload),
                            StandardCharsets.UTF_8
                    ))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "ElevenLabs API가 HTTP " + response.statusCode() + " 응답을 반환했습니다."
                );
            }

            JsonNode body = objectMapper.readTree(response.body());
            String audio = body.path("audio_base64").asText("");
            if (audio.isBlank()) {
                throw new IllegalStateException("ElevenLabs API 응답에 음성이 없습니다.");
            }
            Files.write(audioFile, Base64.getDecoder().decode(audio));
            double duration = mediaProbe.durationSeconds(audioFile);
            List<TimedCaption> captions = captionsFromAlignment(body.path("alignment"), duration);
            if (captions.isEmpty()) captions = captionBuilder.fallback(narration, duration);
            return new VoiceTrack(audioFile, duration, captions, name());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("ElevenLabs 음성 생성이 중단되었습니다.", e);
        } catch (Exception e) {
            throw new IllegalStateException("ElevenLabs 음성 생성에 실패했습니다: " + safeMessage(e), e);
        }
    }

    String styledText(String narration, VideoVoiceStyle style) {
        return switch (style) {
            case WHISPER -> "[whispers] " + narration;
            case SNARKY -> "[sarcastically] " + narration;
            case NATURAL -> narration;
        };
    }

    List<TimedCaption> captionsFromAlignment(JsonNode alignment, double duration) {
        JsonNode characters = alignment.path("characters");
        JsonNode starts = alignment.path("character_start_times_seconds");
        JsonNode ends = alignment.path("character_end_times_seconds");
        if (!characters.isArray() || !starts.isArray() || !ends.isArray()) return List.of();
        int count = Math.min(characters.size(), Math.min(starts.size(), ends.size()));
        if (count == 0) return List.of();

        List<TimedCaption> captions = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        double start = -1;
        double end = 0;
        for (int index = 0; index < count; index++) {
            String value = characters.get(index).asText("");
            if (value.isEmpty()) continue;
            if (start < 0) start = Math.max(0, starts.get(index).asDouble(0));
            if (text.length() + value.length() > 18 && !text.toString().trim().isEmpty()) {
                captions.add(new TimedCaption(start, Math.max(start + 0.1, end), text.toString().trim()));
                text.setLength(0);
                start = Math.max(0, starts.get(index).asDouble(end));
            }
            text.append(value);
            end = Math.max(start + 0.1, ends.get(index).asDouble(start + 0.1));
        }
        if (!text.toString().trim().isEmpty()) {
            captions.add(new TimedCaption(start, Math.max(end, duration), text.toString().trim()));
        }
        return captions;
    }

    private String safeMessage(Exception error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) return error.getClass().getSimpleName();
        String normalized = message.replaceAll("[\\r\\n\\t]+", " ").trim();
        return normalized.length() <= 240 ? normalized : normalized.substring(0, 240);
    }
}
