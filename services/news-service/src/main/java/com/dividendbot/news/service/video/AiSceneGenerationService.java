package com.dividendbot.news.service.video;

import com.dividendbot.news.dto.AiSceneGenerationJobResponse;
import com.dividendbot.news.dto.AiSceneGenerationRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Service
public class AiSceneGenerationService {

    private static final Duration RETENTION = Duration.ofHours(24);

    private final AiSceneProvider provider;
    private final VideoAssetStorage assetStorage;
    private final Executor executor;
    private final Map<UUID, MutableJob> jobs = new ConcurrentHashMap<>();

    public AiSceneGenerationService(
            AiSceneProvider provider,
            VideoAssetStorage assetStorage,
            @Qualifier("aiSceneExecutor") Executor executor
    ) {
        this.provider = provider;
        this.assetStorage = assetStorage;
        this.executor = executor;
    }

    public boolean configured() {
        return provider.configured();
    }

    public String providerName() {
        return provider.name();
    }

    public synchronized AiSceneGenerationJobResponse submit(AiSceneGenerationRequest request) {
        if (!provider.configured()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Higgsfield API 키와 비용 사용 설정이 필요합니다. 기존 사진·영상 또는 스톡 장면 렌더는 계속 사용할 수 있습니다."
            );
        }
        cleanup();
        long generatedForExperiment = jobs.values().stream()
                .filter(job -> job.experimentId.equals(request.experimentId()))
                .filter(job -> !"FAILED".equals(job.status))
                .count();
        if (generatedForExperiment >= 2) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "비용과 과도한 AI 느낌을 줄이기 위해 한 영상에서 AI 장면은 최대 2개만 만들 수 있습니다."
            );
        }
        boolean duplicateScene = jobs.values().stream()
                .anyMatch(job -> job.experimentId.equals(request.experimentId())
                        && job.sceneOrder == request.sceneOrder()
                        && !"FAILED".equals(job.status));
        if (duplicateScene) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이 장면의 AI 생성 작업이 이미 존재합니다.");
        }
        UUID id = UUID.randomUUID();
        MutableJob job = new MutableJob(id, request.experimentId(), request.sceneOrder(), provider.name());
        jobs.put(id, job);
        try {
            executor.execute(() -> generate(job, request));
        } catch (RuntimeException exception) {
            jobs.remove(id);
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "AI 장면 생성 대기열이 가득 찼습니다. 잠시 후 다시 시도해주세요.",
                    exception
            );
        }
        return job.response();
    }

    public AiSceneGenerationJobResponse get(UUID id) {
        return requireJob(id).response();
    }

    public Resource getFile(UUID id) {
        MutableJob job = requireJob(id);
        if (!"COMPLETED".equals(job.status) || job.asset == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "AI 장면 영상이 아직 완료되지 않았습니다.");
        }
        return new FileSystemResource(job.asset.path());
    }

    public String fileName(UUID id) {
        MutableJob job = requireJob(id);
        return job.fileName == null ? "ai-scene.mp4" : job.fileName;
    }

    private void generate(MutableJob job, AiSceneGenerationRequest request) {
        try {
            job.update("GENERATING", "AI 이미지와 동작 구성", 10);
            AiSceneProvider.GeneratedSceneVideo generated = provider.generate(request);
            job.update("GENERATING", "생성 영상 안전 저장", 90);
            VideoAssetStorage.StoredVideoAsset stored = assetStorage.storeGenerated(
                    generated.data(),
                    generated.contentType()
            );
            job.complete(stored, generated.fileName());
        } catch (Exception exception) {
            job.fail(safeMessage(exception));
        }
    }

    private MutableJob requireJob(UUID id) {
        MutableJob job = jobs.get(id);
        if (job == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "AI 장면 작업을 찾을 수 없습니다.");
        }
        return job;
    }

    private void cleanup() {
        Instant threshold = Instant.now().minus(RETENTION);
        jobs.entrySet().removeIf(entry -> entry.getValue().updatedAt.isBefore(threshold));
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) return "AI 장면 생성에 실패했습니다.";
        String normalized = message.replaceAll("[\\r\\n]+", " ").trim();
        return normalized.length() > 300 ? normalized.substring(0, 300) : normalized;
    }

    private static final class MutableJob {
        private final UUID id;
        private final String experimentId;
        private final int sceneOrder;
        private final String provider;
        private final Instant createdAt;
        private volatile String status;
        private volatile String stage;
        private volatile int progress;
        private volatile VideoAssetStorage.StoredVideoAsset asset;
        private volatile String fileName;
        private volatile String errorMessage;
        private volatile Instant updatedAt;
        private volatile Instant completedAt;

        private MutableJob(UUID id, String experimentId, int sceneOrder, String provider) {
            this.id = id;
            this.experimentId = experimentId;
            this.sceneOrder = sceneOrder;
            this.provider = provider;
            this.createdAt = Instant.now();
            this.updatedAt = createdAt;
            this.status = "QUEUED";
            this.stage = "AI 장면 생성 대기";
            this.progress = 0;
        }

        private void update(String status, String stage, int progress) {
            this.status = status;
            this.stage = stage;
            this.progress = progress;
            this.updatedAt = Instant.now();
        }

        private void complete(VideoAssetStorage.StoredVideoAsset asset, String fileName) {
            this.asset = asset;
            this.fileName = fileName;
            this.status = "COMPLETED";
            this.stage = "AI 장면 준비 완료";
            this.progress = 100;
            this.updatedAt = Instant.now();
            this.completedAt = updatedAt;
        }

        private void fail(String message) {
            this.status = "FAILED";
            this.stage = "AI 장면 생성 실패";
            this.progress = 100;
            this.errorMessage = message;
            this.updatedAt = Instant.now();
            this.completedAt = updatedAt;
        }

        private AiSceneGenerationJobResponse response() {
            return new AiSceneGenerationJobResponse(
                    id,
                    sceneOrder,
                    status,
                    stage,
                    progress,
                    provider,
                    asset == null ? null : asset.reference(),
                    asset == null ? null : asset.mediaKind().name(),
                    asset == null ? null : asset.contentType(),
                    fileName,
                    errorMessage,
                    createdAt,
                    updatedAt,
                    completedAt
            );
        }
    }
}
