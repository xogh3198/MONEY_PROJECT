package com.dividendbot.news.service.video;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Component
public class ShortformSourceStorage {
    public static final long MAX_SOURCE_BYTES = 200L * 1024 * 1024;
    public static final int MAX_CHUNK_BYTES = 3 * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of(
            "video/mp4", "mp4", "video/webm", "webm", "video/quicktime", "mov"
    );
    private final Path root;
    private final MediaProbe probe;

    public ShortformSourceStorage(@Value("${video.render.storage-path:/var/lib/investboard/videos}") String storagePath, MediaProbe probe) {
        root = Path.of(storagePath).toAbsolutePath().normalize().resolve("shortform-sources");
        this.probe = probe;
    }

    public synchronized UUID start(long size, String contentType) {
        if (size <= 0 || size > MAX_SOURCE_BYTES) throw new IllegalArgumentException("원본 영상은 200MB 이하여야 합니다.");
        String extension = EXTENSIONS.get(contentType);
        if (extension == null) throw new IllegalArgumentException("MP4, WebM, MOV 원본 영상만 업로드할 수 있습니다.");
        try {
            Files.createDirectories(root);
            cleanupExpired();
            try (var entries = Files.list(root)) {
                if (entries.filter(Files::isDirectory).count() >= 8) throw new IllegalArgumentException("동시 업로드가 많습니다. 잠시 후 다시 시도해주세요.");
            }
            UUID id = UUID.randomUUID();
            Path directory = directory(id);
            Files.createDirectory(directory);
            Files.writeString(directory.resolve("meta"), size + "\n" + extension);
            return id;
        } catch (IOException error) {
            throw new IllegalStateException("원본 업로드를 시작하지 못했습니다.", error);
        }
    }

    public synchronized long append(UUID id, long offset, byte[] chunk) {
        Metadata metadata = metadata(id);
        if (Files.exists(directory(id).resolve("ready"))) throw new IllegalArgumentException("이미 완료된 업로드입니다.");
        if (chunk == null || chunk.length == 0 || chunk.length > MAX_CHUNK_BYTES) throw new IllegalArgumentException("업로드 조각은 3MB 이하여야 합니다.");
        Path target = sourcePath(id, metadata.extension());
        try {
            long written = Files.exists(target) ? Files.size(target) : 0;
            if (written != offset || written + chunk.length > metadata.size()) throw new IllegalArgumentException("업로드 순서나 크기가 올바르지 않습니다.");
            Files.write(target, chunk, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return written + chunk.length;
        } catch (IOException error) {
            throw new IllegalStateException("원본 업로드 조각을 저장하지 못했습니다.", error);
        }
    }

    public synchronized Source complete(UUID id) {
        Metadata metadata = metadata(id);
        Path file = sourcePath(id, metadata.extension());
        try {
            if (!Files.isRegularFile(file) || Files.size(file) != metadata.size()) throw new IllegalArgumentException("업로드한 파일 크기가 일치하지 않습니다.");
            double duration = probe.durationSeconds(file);
            if (!Double.isFinite(duration) || duration < 5 || duration > 3600) throw new IllegalArgumentException("원본 영상 길이는 5초~60분이어야 합니다.");
            Files.writeString(directory(id).resolve("ready"), Double.toString(duration));
            return new Source(id, file, duration);
        } catch (IOException error) {
            throw new IllegalStateException("업로드를 검증하지 못했습니다.", error);
        }
    }

    public Source resolve(UUID id) {
        Metadata metadata = metadata(id);
        Path ready = directory(id).resolve("ready");
        Path file = sourcePath(id, metadata.extension());
        if (!Files.isRegularFile(ready) || !Files.isRegularFile(file)) throw new IllegalArgumentException("완료된 원본 업로드가 아닙니다.");
        try {
            return new Source(id, file, Double.parseDouble(Files.readString(ready)));
        } catch (IOException | NumberFormatException error) {
            throw new IllegalStateException("원본 영상 정보를 읽지 못했습니다.", error);
        }
    }

    private Metadata metadata(UUID id) {
        try {
            String[] parts = Files.readString(directory(id).resolve("meta")).split("\\n");
            return new Metadata(Long.parseLong(parts[0]), parts[1].trim());
        } catch (IOException | RuntimeException error) {
            throw new IllegalArgumentException("원본 업로드 ID를 찾을 수 없습니다.", error);
        }
    }

    private Path directory(UUID id) { return root.resolve(id.toString()); }
    private Path sourcePath(UUID id, String extension) { return directory(id).resolve("source." + extension); }

    private void cleanupExpired() throws IOException {
        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        try (var entries = Files.list(root)) {
            for (Path entry : entries.toList()) {
                if (!Files.isDirectory(entry) || !Files.getLastModifiedTime(entry).toInstant().isBefore(cutoff)) continue;
                try (var files = Files.list(entry)) {
                    for (Path file : files.toList()) Files.deleteIfExists(file);
                }
                Files.deleteIfExists(entry);
            }
        }
    }

    private record Metadata(long size, String extension) {}
    public record Source(UUID id, Path path, double durationSeconds) {}
}
