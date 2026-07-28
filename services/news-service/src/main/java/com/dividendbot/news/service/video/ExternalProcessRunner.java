package com.dividendbot.news.service.video;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class ExternalProcessRunner {

    public String run(List<String> command, Duration timeout) {
        Process process = null;
        try {
            process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            boolean completed = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new IllegalStateException("외부 처리 시간이 제한을 초과했습니다.");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                throw new IllegalStateException("외부 처리 실패: " + sanitize(output));
            }
            return output.trim();
        } catch (IOException e) {
            throw new IllegalStateException("필수 미디어 도구를 실행할 수 없습니다.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("외부 처리가 중단되었습니다.", e);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private String sanitize(String output) {
        String normalized = output == null ? "" : output.replaceAll("[\\r\\n\\t]+", " ").trim();
        if (normalized.isBlank()) return "상세 출력 없음";
        return normalized.length() <= 800 ? normalized : normalized.substring(normalized.length() - 800);
    }
}
