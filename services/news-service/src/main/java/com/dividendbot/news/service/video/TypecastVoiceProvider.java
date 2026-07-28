package com.dividendbot.news.service.video;

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

@Component
public class TypecastVoiceProvider implements VoiceProvider {

    private static final URI API_URI = URI.create(
            "https://api.typecast.ai/v1/text-to-speech/with-timestamps?granularity=word"
    );

    private final ObjectMapper objectMapper;
    private final SpeechMarkCaptionBuilder captionBuilder;
    private final String apiKey;
    private final String voiceId;
    private final HttpClient httpClient;

    public TypecastVoiceProvider(
            ObjectMapper objectMapper,
            SpeechMarkCaptionBuilder captionBuilder,
            @Value("${video.voice.typecast-api-key:}") String apiKey,
            @Value("${video.voice.typecast-voice-id:}") String voiceId
    ) {
        this.objectMapper = objectMapper;
        this.captionBuilder = captionBuilder;
        this.apiKey = apiKey;
        this.voiceId = voiceId;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String name() {
        return "TYPECAST";
    }

    @Override
    public boolean configured() {
        return apiKey != null && !apiKey.isBlank()
                && voiceId != null && !voiceId.isBlank();
    }

    @Override
    public VoiceTrack synthesize(String narration, Path outputDirectory, String fileStem) {
        if (!configured()) throw new IllegalStateException("Typecast API 설정이 비어 있습니다.");
        try {
            Files.createDirectories(outputDirectory);
            Path audioFile = outputDirectory.resolve(fileStem + ".wav");

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("voice_id", voiceId);
            payload.put("text", narration);
            payload.put("model", "ssfm-v30");
            payload.put("language", "kor");
            payload.put("prompt", Map.of(
                    "emotion_type", "preset",
                    "emotion_preset", "normal",
                    "emotion_intensity", 1.15
            ));
            payload.put("output", Map.of(
                    "target_lufs", -14,
                    "volume", 100,
                    "audio_pitch", 0,
                    "audio_tempo", 1.03,
                    "audio_format", "wav"
            ));

            HttpRequest request = HttpRequest.newBuilder(API_URI)
                    .timeout(Duration.ofSeconds(90))
                    .header("Content-Type", "application/json")
                    .header("X-API-KEY", apiKey)
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
                        "Typecast API가 HTTP " + response.statusCode() + " 응답을 반환했습니다."
                );
            }

            JsonNode body = objectMapper.readTree(response.body());
            String audio = body.path("audio").asText("");
            double duration = body.path("audio_duration").asDouble(0);
            if (audio.isBlank() || duration <= 0) {
                throw new IllegalStateException("Typecast API 응답에 음성 또는 재생 시간이 없습니다.");
            }
            Files.write(audioFile, Base64.getDecoder().decode(audio));

            List<TimedCaption> captions = captionsFromWords(body.path("words"), duration);
            if (captions.isEmpty()) captions = captionBuilder.fallback(narration, duration);
            return new VoiceTrack(audioFile, duration, captions, name());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Typecast 음성 생성이 중단되었습니다.", e);
        } catch (Exception e) {
            throw new IllegalStateException("Typecast 음성 생성에 실패했습니다: " + safeMessage(e), e);
        }
    }

    private List<TimedCaption> captionsFromWords(JsonNode words, double duration) {
        if (!words.isArray() || words.isEmpty()) return List.of();

        List<TimedCaption> captions = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        double start = -1;
        double end = 0;
        for (JsonNode word : words) {
            String value = word.path("text").asText("").trim();
            if (value.isBlank()) continue;
            if (start < 0) start = Math.max(0, word.path("start").asDouble(0));

            String separator = text.isEmpty() ? "" : " ";
            if (!text.isEmpty() && text.length() + separator.length() + value.length() > 18) {
                captions.add(new TimedCaption(start, Math.max(start + 0.1, end), text.toString()));
                text.setLength(0);
                start = Math.max(0, word.path("start").asDouble(end));
            }
            if (!text.isEmpty()) text.append(' ');
            text.append(value);
            end = Math.max(start + 0.1, word.path("end").asDouble(start + 0.1));
        }
        if (!text.isEmpty()) {
            captions.add(new TimedCaption(start, Math.max(end, duration), text.toString()));
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
