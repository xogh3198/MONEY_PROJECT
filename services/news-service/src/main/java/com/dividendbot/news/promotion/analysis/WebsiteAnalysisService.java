package com.dividendbot.news.promotion.analysis;

import com.dividendbot.news.promotion.common.ApiException;
import com.dividendbot.news.promotion.security.UrlSafetyValidator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.dividendbot.news.promotion.analysis.WebsiteAnalysisModels.CreateRequest;
import static com.dividendbot.news.promotion.analysis.WebsiteAnalysisModels.Evidence;
import static com.dividendbot.news.promotion.analysis.WebsiteAnalysisModels.Response;

@Service
public class WebsiteAnalysisService {

    private static final Set<String> SOURCE_TYPES =
            Set.of("URL", "TEXT", "PRODUCT", "PLACE", "APP", "CONTENT");

    private final UrlSafetyValidator urlSafetyValidator;
    private final Map<String, Response> analyses = new ConcurrentHashMap<>();

    public WebsiteAnalysisService(UrlSafetyValidator urlSafetyValidator) {
        this.urlSafetyValidator = urlSafetyValidator;
    }

    public Response create(CreateRequest request) {
        String sourceType = normalizeSourceType(request.sourceType());
        if ("URL".equals(sourceType) && isBlank(request.url())) {
            throw invalid("URL 방식에는 사이트 주소가 필요합니다.");
        }
        if (!"URL".equals(sourceType) && isBlank(request.title()) && isBlank(request.description())) {
            throw invalid("홍보할 대상의 이름이나 소개 내용을 입력해 주세요.");
        }

        URI canonical = isBlank(request.url())
                ? null
                : urlSafetyValidator.validateAndNormalize(request.url());
        List<URI> references = validateReferences(request.referenceLinks());
        String id = UUID.randomUUID().toString();
        String sourceUrl = canonical == null ? "" : canonical.toString();
        String title = !isBlank(request.title())
                ? request.title().trim()
                : canonical == null ? sourceTypeLabel(sourceType) : canonical.getHost();
        String summary = !isBlank(request.description())
                ? request.description().trim()
                : "공개 URL을 기준으로 홍보 브리프를 시작합니다.";

        List<Evidence> evidence = new ArrayList<>();
        evidence.add(new Evidence("입력 방식", sourceTypeLabel(sourceType), sourceUrl));
        if (canonical != null) {
            evidence.add(new Evidence("정규 URL", sourceUrl, sourceUrl));
            evidence.add(new Evidence("호스트", canonical.getHost(), sourceUrl));
        }
        references.forEach(reference ->
                evidence.add(new Evidence("참고 링크", reference.toString(), reference.toString())));

        Response response = new Response(
                id,
                sourceType,
                sourceUrl,
                "NEEDS_CONFIRMATION",
                title,
                truncate(summary, 500),
                "확인 필요",
                List.of("홍보 대상의 핵심 고객"),
                List.of("방문, 문의, 가입 또는 구매"),
                List.of("전국 또는 직접 입력"),
                evidence,
                List.of(
                        "현재 단계에서는 외부 페이지 본문이나 첨부파일을 자동 수집하지 않았습니다.",
                        "업종, 고객, CTA는 다음 분석 단계에서 근거와 함께 확인해야 합니다."
                ),
                Instant.now()
        );
        analyses.put(id, response);
        return response;
    }

    private String normalizeSourceType(String rawSourceType) {
        String normalized = isBlank(rawSourceType)
                ? "URL"
                : rawSourceType.trim().toUpperCase(Locale.ROOT);
        if (!SOURCE_TYPES.contains(normalized)) {
            throw invalid("지원하지 않는 입력 방식입니다.");
        }
        return normalized;
    }

    private List<URI> validateReferences(List<String> rawReferences) {
        if (rawReferences == null) {
            return List.of();
        }
        return rawReferences.stream()
                .filter(reference -> !isBlank(reference))
                .map(urlSafetyValidator::validateAndNormalize)
                .toList();
    }

    private String sourceTypeLabel(String sourceType) {
        return switch (sourceType) {
            case "TEXT" -> "소개글";
            case "PRODUCT" -> "상품·서비스";
            case "PLACE" -> "매장·장소";
            case "APP" -> "앱";
            case "CONTENT" -> "콘텐츠";
            default -> "웹사이트 URL";
        };
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PROMOTION_SOURCE", message);
    }

    public Response getRequired(String analysisId) {
        Response response = analyses.get(analysisId);
        if (response == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "ANALYSIS_NOT_FOUND",
                    "분석 요청을 찾을 수 없습니다."
            );
        }
        return response;
    }
}
