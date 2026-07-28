package com.dividendbot.news.domain.repository;

import com.dividendbot.news.domain.entity.VideoRenderJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VideoRenderJobRepository extends JpaRepository<VideoRenderJob, UUID> {
}
