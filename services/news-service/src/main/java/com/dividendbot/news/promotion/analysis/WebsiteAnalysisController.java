package com.dividendbot.news.promotion.analysis;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static com.dividendbot.news.promotion.analysis.WebsiteAnalysisModels.CreateRequest;
import static com.dividendbot.news.promotion.analysis.WebsiteAnalysisModels.Response;

@RestController
@RequestMapping({"/api/v1/promotion-sources", "/api/v1/website-analyses"})
public class WebsiteAnalysisController {

    private final WebsiteAnalysisService service;

    public WebsiteAnalysisController(WebsiteAnalysisService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Response create(@Valid @RequestBody CreateRequest request) {
        return service.create(request);
    }

    @GetMapping("/{analysisId}")
    public Response get(@PathVariable String analysisId) {
        return service.getRequired(analysisId);
    }
}
