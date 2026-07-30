package com.dividendbot.news.promotion.security;

import com.dividendbot.news.promotion.common.ApiException;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UrlSafetyValidatorTest {

    private final UrlSafetyValidator validator = new UrlSafetyValidator();

    @Test
    void acceptsAndNormalizesPublicHttpUrl() {
        URI result = validator.validateAndNormalize(
                "https://Example.com/products/?token=secret#section"
        );

        assertThat(result.toString()).isEqualTo("https://example.com/products/");
    }

    @Test
    void rejectsUnsupportedAndPrivateAddresses() {
        assertThatThrownBy(() -> validator.validateAndNormalize("file:///etc/passwd"))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> validator.validateAndNormalize("https://user:pass@example.com"))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> validator.validateAndNormalize("http://localhost:8080"))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> validator.validateAndNormalize("http://127.0.0.1"))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> validator.validateAndNormalize("http://169.254.169.254/latest"))
                .isInstanceOf(ApiException.class);
    }
}
