package com.dividendbot.news.service.video;

import com.dividendbot.news.dto.AiSceneGenerationRequest;

public interface AiSceneProvider {

    boolean configured();

    String name();

    GeneratedSceneVideo generate(AiSceneGenerationRequest request);

    record GeneratedSceneVideo(
            byte[] data,
            String contentType,
            String fileName,
            String credit
    ) {
    }
}
