package com.dividendbot.news.service.video;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class VideoAssetStorage {

    private static final long MAX_UPLOAD_BYTES = 25L * 1024 * 1024;
    private static final Pattern SAFE_REFERENCE = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp|mp4|webm|mov)$"
    );
    private static final Map<String, AssetType> ALLOWED_TYPES = Map.of(
            "image/jpeg", new AssetType("jpg", SceneMediaKind.IMAGE),
            "image/png", new AssetType("png", SceneMediaKind.IMAGE),
            "image/webp", new AssetType("webp", SceneMediaKind.IMAGE),
            "video/mp4", new AssetType("mp4", SceneMediaKind.VIDEO),
            "video/webm", new AssetType("webm", SceneMediaKind.VIDEO),
            "video/quicktime", new AssetType("mov", SceneMediaKind.VIDEO)
    );

    private final Path uploadRoot;

    public VideoAssetStorage(
            @Value("${video.render.storage-path:/var/lib/investboard/videos}") String storagePath
    ) {
        this.uploadRoot = Path.of(storagePath).toAbsolutePath().normalize().resolve("uploads");
    }

    public StoredVideoAsset store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 사진 또는 영상 파일이 필요합니다.");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new IllegalArgumentException("장면 파일은 25MB 이하여야 합니다.");
        }
        String contentType = Optional.ofNullable(file.getContentType())
                .orElse("")
                .toLowerCase(Locale.ROOT);
        AssetType assetType = ALLOWED_TYPES.get(contentType);
        if (assetType == null) {
            throw new IllegalArgumentException("JPG, PNG, WebP, MP4, WebM, MOV 파일만 사용할 수 있습니다.");
        }

        String reference = UUID.randomUUID() + "." + assetType.extension();
        Path target = safePath(reference);
        try {
            Files.createDirectories(uploadRoot);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return new StoredVideoAsset(reference, target, assetType.mediaKind(), contentType);
        } catch (IOException e) {
            throw new IllegalStateException("장면 파일을 저장하지 못했습니다.", e);
        }
    }

    public StoredVideoAsset resolve(String reference) {
        if (reference == null || !SAFE_REFERENCE.matcher(reference).matches()) {
            throw new IllegalArgumentException("잘못된 장면 파일 참조입니다.");
        }
        Path path = safePath(reference);
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("업로드한 장면 파일을 찾을 수 없습니다.");
        }
        String extension = reference.substring(reference.lastIndexOf('.') + 1);
        SceneMediaKind mediaKind = switch (extension) {
            case "jpg", "png", "webp" -> SceneMediaKind.IMAGE;
            case "mp4", "webm", "mov" -> SceneMediaKind.VIDEO;
            default -> throw new IllegalArgumentException("지원하지 않는 장면 파일 형식입니다.");
        };
        String contentType = switch (extension) {
            case "jpg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "mp4" -> "video/mp4";
            case "webm" -> "video/webm";
            case "mov" -> "video/quicktime";
            default -> "application/octet-stream";
        };
        return new StoredVideoAsset(reference, path, mediaKind, contentType);
    }

    private Path safePath(String reference) {
        Path path = uploadRoot.resolve(reference).normalize();
        if (!path.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("잘못된 장면 파일 경로입니다.");
        }
        return path;
    }

    private record AssetType(String extension, SceneMediaKind mediaKind) {
    }

    public record StoredVideoAsset(
            String reference,
            Path path,
            SceneMediaKind mediaKind,
            String contentType
    ) {
    }
}
