package com.dividendbot.news.service.video;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpeechMarkCaptionBuilderTest {

    private final SpeechMarkCaptionBuilder builder = new SpeechMarkCaptionBuilder(new ObjectMapper());

    @Test
    void buildsTimedCaptionGroupsFromPollySpeechMarks() {
        String marks = """
                {"time":0,"type":"word","value":"오늘"}
                {"time":310,"type":"word","value":"증시에서"}
                {"time":820,"type":"word","value":"꼭"}
                {"time":1040,"type":"word","value":"봐야"}
                {"time":1370,"type":"word","value":"할"}
                {"time":1510,"type":"word","value":"숫자입니다."}
                """;

        List<TimedCaption> captions = builder.fromPollyJsonLines(marks, 2.4);

        assertThat(captions).isNotEmpty();
        assertThat(captions.get(0).startSeconds()).isZero();
        assertThat(captions.get(captions.size() - 1).endSeconds()).isEqualTo(2.4);
        assertThat(captions)
                .extracting(TimedCaption::text)
                .allSatisfy(text -> assertThat(text.length()).isLessThanOrEqualTo(22));
    }

    @Test
    void fallsBackToLengthWeightedCaptions() {
        List<TimedCaption> captions = builder.fallback(
                "첫 번째 핵심입니다. 다음으로 확인할 내용도 있습니다.",
                4.0
        );

        assertThat(captions).hasSizeGreaterThanOrEqualTo(2);
        assertThat(captions.get(0).startSeconds()).isZero();
        assertThat(captions.get(captions.size() - 1).endSeconds()).isEqualTo(4.0);
    }
}
