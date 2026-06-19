package com.dividendbot.news.config;

import com.dividendbot.news.domain.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 앱 시작 시 더미 데이터 정리.
 * 이전 시드 데이터(example.com URL)가 있으면 삭제.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final NewsArticleRepository newsRepository;

    @Override
    public void run(String... args) {
        // 더미 데이터 정리 (example.com URL을 가진 시드 기사 삭제)
        long deleted = newsRepository.findAll().stream()
                .filter(a -> a.getSourceUrl() != null && a.getSourceUrl().contains("example.com"))
                .peek(a -> newsRepository.delete(a))
                .count();

        if (deleted > 0) {
            log.info("더미 뉴스 데이터 {}건 삭제 완료", deleted);
        }
    }
}
