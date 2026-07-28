package com.dividendbot.news.service.video;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

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

    public VoiceTrack synthesize(String narration, Path outputDirectory, String fileStem) {
        VoiceProvider provider = selected();
        if (!provider.configured()) {
            throw new IllegalStateException(provider.name() + " 음성 공급자 설정이 완료되지 않았습니다.");
        }
        return provider.synthesize(narration, outputDirectory, fileStem);
    }

    public String selectedName() {
        return selectedProvider;
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

    private VoiceProvider selected() {
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
}
