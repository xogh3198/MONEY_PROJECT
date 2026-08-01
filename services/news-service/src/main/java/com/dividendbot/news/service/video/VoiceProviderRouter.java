package com.dividendbot.news.service.video;

import com.dividendbot.news.domain.entity.VideoVoiceStyle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class VoiceProviderRouter {

    private final List<VoiceProvider> providers;
    private final String selectedProvider;

    public VoiceProviderRouter(
            List<VoiceProvider> providers,
            @Value("${video.voice.provider:POLLY}") String selectedProvider
    ) {
        this.providers = providers;
        this.selectedProvider = normalize(selectedProvider);
    }

    public VoiceTrack synthesize(
            String narration,
            Path outputDirectory,
            String fileStem,
            VideoVoiceStyle voiceStyle
    ) {
        return synthesize(narration, outputDirectory, fileStem, voiceStyle, null, null);
    }

    public VoiceTrack synthesize(
            String narration,
            Path outputDirectory,
            String fileStem,
            VideoVoiceStyle voiceStyle,
            String requestedProvider,
            String requestedVoiceId
    ) {
        VoiceProvider provider;
        if (requestedProvider != null && !requestedProvider.isBlank()) {
            provider = providers.stream()
                    .filter(p -> p.name().equalsIgnoreCase(requestedProvider.trim()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "지원하지 않는 음성 공급자입니다: " + requestedProvider
                    ));
        } else {
            provider = selected();
        }
        if (!provider.configured()) {
            throw new IllegalStateException(provider.name() + " 음성 공급자 설정이 완료되지 않았습니다.");
        }
        if (voiceStyle != VideoVoiceStyle.NATURAL && !expressive(provider)) {
            throw new IllegalStateException(
                    "속삭임·시니컬 음성은 ElevenLabs 또는 Typecast API 설정 후 사용할 수 있습니다."
            );
        }
        return provider.synthesize(narration, outputDirectory, fileStem, voiceStyle, requestedVoiceId);
    }

    public String selectedName() {
        return selected().name();
    }

    public boolean selectedConfigured() {
        return selected().configured();
    }

    public List<String> availableProviders() {
        return providers.stream()
                .filter(VoiceProvider::configured)
                .map(VoiceProvider::name)
                .sorted()
                .toList();
    }

    public List<String> supportedStyles() {
        if (expressive(selected())) {
            return List.of("NATURAL", "WHISPER", "SNARKY");
        }
        return List.of("NATURAL");
    }

    public List<Map<String, Object>> voiceCatalog() {
        List<Map<String, Object>> catalog = new java.util.ArrayList<>();
        for (VoiceProvider provider : providers) {
            Map<String, Object> entry = new java.util.LinkedHashMap<>();
            entry.put("id", provider.name());
            entry.put("name", provider.displayName());
            entry.put("tier", provider.tier());
            entry.put("configured", provider.configured());
            entry.put("voices", provider.availableVoices());
            catalog.add(entry);
        }
        return catalog;
    }

    private VoiceProvider selected() {
        if (selectedProvider.equals("AUTO")) {
            return providers.stream()
                    .filter(VoiceProvider::configured)
                    .sorted((left, right) -> Integer.compare(priority(left), priority(right)))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("사용 가능한 음성 공급자 설정이 없습니다."));
        }
        return providers.stream()
                .filter(provider -> provider.name().equalsIgnoreCase(selectedProvider))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "지원하지 않는 음성 공급자입니다: " + selectedProvider
                ));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) return "POLLY";
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private int priority(VoiceProvider provider) {
        if (provider.name().equalsIgnoreCase("ELEVENLABS")) return 0;
        if (provider.name().equalsIgnoreCase("TYPECAST")) return 1;
        if (provider.name().equalsIgnoreCase("POLLY")) return 2;
        return 10;
    }

    private boolean expressive(VoiceProvider provider) {
        return provider.name().equalsIgnoreCase("ELEVENLABS")
                || provider.name().equalsIgnoreCase("TYPECAST");
    }
}
