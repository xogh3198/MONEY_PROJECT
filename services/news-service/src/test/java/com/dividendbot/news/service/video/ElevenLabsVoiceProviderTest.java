package com.dividendbot.news.service.video;

import com.dividendbot.news.domain.entity.VideoVoiceStyle;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ElevenLabsVoiceProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ElevenLabsVoiceProvider provider = new ElevenLabsVoiceProvider(
            objectMapper,
            mock(SpeechMarkCaptionBuilder.class),
            mock(MediaProbe.class),
            "api-key",
            "voice_123456",
            "eleven_v3"
    );

    @Test
    void mapsCharacterStylesToExpressiveV3Tags() {
        assertThat(provider.styledText("궁금하지 않으세요?", VideoVoiceStyle.WHISPER))
                .startsWith("[whispers] ");
        assertThat(provider.styledText("또 이걸 직접 하고 계세요?", VideoVoiceStyle.SNARKY))
                .startsWith("[sarcastically] ");
        assertThat(provider.styledText("핵심만 설명합니다.", VideoVoiceStyle.NATURAL))
                .isEqualTo("핵심만 설명합니다.");
    }

    @Test
    void createsCaptionGroupsFromCharacterAlignment() throws Exception {
        JsonNode alignment = objectMapper.readTree("""
                {
                  "characters": ["안", "녕", "하", "세", "요", "."],
                  "character_start_times_seconds": [0, 0.1, 0.2, 0.3, 0.4, 0.5],
                  "character_end_times_seconds": [0.1, 0.2, 0.3, 0.4, 0.5, 0.6]
                }
                """);

        List<TimedCaption> captions = provider.captionsFromAlignment(alignment, 0.6);

        assertThat(captions).hasSize(1);
        assertThat(captions.get(0).text()).isEqualTo("안녕하세요.");
        assertThat(captions.get(0).startSeconds()).isZero();
        assertThat(captions.get(0).endSeconds()).isEqualTo(0.6);
    }
}
