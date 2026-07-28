package com.dividendbot.news.service.video;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AssSubtitleWriterTest {

    @TempDir
    Path tempDirectory;

    @Test
    void writesVerticalVideoSubtitleTrack() throws Exception {
        AssSubtitleWriter writer = new AssSubtitleWriter();
        Path output = tempDirectory.resolve("captions.ass");

        writer.write(
                List.of(
                        new TimedCaption(0, 1.25, "첫 번째 자막"),
                        new TimedCaption(1.25, 2.5, "두 번째 {자막}")
                ),
                540,
                960,
                output
        );

        String contents = Files.readString(output);
        assertThat(contents).contains("PlayResX: 540", "PlayResY: 960");
        assertThat(contents).contains("0:00:00.00,0:00:01.25");
        assertThat(contents).contains("두 번째 \\{자막\\}");
    }
}
