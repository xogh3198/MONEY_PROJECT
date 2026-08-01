package com.dividendbot.news.service.video;

import com.dividendbot.news.domain.entity.VideoVoiceStyle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.polly.model.Engine;
import software.amazon.awssdk.services.polly.model.OutputFormat;
import software.amazon.awssdk.services.polly.model.SpeechMarkType;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechRequest;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Component
public class PollyVoiceProvider implements VoiceProvider {

    private final MediaProbe mediaProbe;
    private final SpeechMarkCaptionBuilder captionBuilder;
    private final String region;
    private final String voiceId;
    private final String engine;

    public PollyVoiceProvider(
            MediaProbe mediaProbe,
            SpeechMarkCaptionBuilder captionBuilder,
            @Value("${video.voice.aws-region:ap-northeast-2}") String region,
            @Value("${video.voice.polly-voice-id:Jihye}") String voiceId,
            @Value("${video.voice.polly-engine:neural}") String engine
    ) {
        this.mediaProbe = mediaProbe;
        this.captionBuilder = captionBuilder;
        this.region = region;
        this.voiceId = voiceId;
        this.engine = engine;
    }

    @Override
    public String name() {
        return "POLLY";
    }

    @Override
    public boolean configured() {
        return !region.isBlank() && !voiceId.isBlank();
    }

    @Override
    public String displayName() {
        return "AWS Polly";
    }

    @Override
    public String tier() {
        return "FREE";
    }

    @Override
    public List<Map<String, Object>> availableVoices() {
        return List.of(
                Map.of("id", "Jihye", "name", "Jihye", "gender", "FEMALE",
                        "description", "차분한 뉴스 앵커 톤",
                        "styles", List.of("NATURAL")),
                Map.of("id", "Seoyeon", "name", "Seoyeon", "gender", "FEMALE",
                        "description", "따뜻하고 친근한 톤",
                        "styles", List.of("NATURAL")),
                Map.of("id", "Sujin", "name", "Sujin", "gender", "FEMALE",
                        "description", "밝고 명랑한 톤",
                        "styles", List.of("NATURAL"))
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
        if (!configured()) throw new IllegalStateException("Amazon Polly 설정이 비어 있습니다.");
        try {
            Files.createDirectories(outputDirectory);
            Path audioFile = outputDirectory.resolve(fileStem + ".mp3");
            String speechMarks;

            try (PollyClient client = PollyClient.builder()
                    .region(Region.of(region))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build()) {
                SynthesizeSpeechRequest base = SynthesizeSpeechRequest.builder()
                        .text(narration)
                        .voiceId(effectiveVoiceId)
                        .engine(Engine.fromValue(engine))
                        .languageCode("ko-KR")
                        .build();

                try (ResponseInputStream<SynthesizeSpeechResponse> audio = client.synthesizeSpeech(
                        base.toBuilder().outputFormat(OutputFormat.MP3).build()
                )) {
                    Files.copy(audio, audioFile);
                }

                try (ResponseInputStream<SynthesizeSpeechResponse> marks = client.synthesizeSpeech(
                        base.toBuilder()
                                .outputFormat(OutputFormat.JSON)
                                .speechMarkTypes(SpeechMarkType.WORD)
                                .build()
                )) {
                    speechMarks = new String(marks.readAllBytes(), StandardCharsets.UTF_8);
                }
            }

            double duration = mediaProbe.durationSeconds(audioFile);
            List<TimedCaption> captions = captionBuilder.fromPollyJsonLines(speechMarks, duration);
            if (captions.isEmpty()) captions = captionBuilder.fallback(narration, duration);
            return new VoiceTrack(audioFile, duration, captions, name());
        } catch (IOException e) {
            throw new IllegalStateException("Polly 음성 파일을 저장하지 못했습니다.", e);
        } catch (Exception e) {
            throw new IllegalStateException("Amazon Polly 음성 생성에 실패했습니다: " + safeMessage(e), e);
        }
    }

    private String safeMessage(Exception error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) return error.getClass().getSimpleName();
        String normalized = message.replaceAll("[\\r\\n\\t]+", " ").trim();
        return normalized.length() <= 300 ? normalized : normalized.substring(0, 300);
    }
}
