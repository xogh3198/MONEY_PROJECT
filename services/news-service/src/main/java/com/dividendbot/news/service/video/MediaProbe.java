package com.dividendbot.news.service.video;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

@Component
public class MediaProbe {

    private final ExternalProcessRunner processRunner;
    private final String ffprobePath;

    public MediaProbe(
            ExternalProcessRunner processRunner,
            @Value("${video.render.ffprobe-path:ffprobe}") String ffprobePath
    ) {
        this.processRunner = processRunner;
        this.ffprobePath = ffprobePath;
    }

    public double durationSeconds(Path file) {
        String output = processRunner.run(
                List.of(
                        ffprobePath,
                        "-v", "error",
                        "-show_entries", "format=duration",
                        "-of", "default=noprint_wrappers=1:nokey=1",
                        file.toAbsolutePath().toString()
                ),
                Duration.ofSeconds(30)
        );
        try {
            return Double.parseDouble(output);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("생성된 오디오 길이를 확인할 수 없습니다.", e);
        }
    }
}
