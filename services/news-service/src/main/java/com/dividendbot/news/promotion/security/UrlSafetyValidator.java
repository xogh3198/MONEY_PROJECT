package com.dividendbot.news.promotion.security;

import com.dividendbot.news.promotion.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.IDN;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class UrlSafetyValidator {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
    private static final Set<String> BLOCKED_HOSTS = Set.of(
            "localhost",
            "localhost.localdomain",
            "metadata.google.internal",
            "169.254.169.254"
    );
    private static final Pattern IPV4_LITERAL =
            Pattern.compile("^\\d{1,3}(?:\\.\\d{1,3}){3}$");

    public URI validateAndNormalize(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw unsafe("사이트 URL을 입력해 주세요.");
        }

        final URI parsed;
        try {
            parsed = new URI(rawUrl.trim());
        } catch (URISyntaxException exception) {
            throw unsafe("올바른 URL 형식이 아닙니다.");
        }

        String scheme = parsed.getScheme() == null
                ? ""
                : parsed.getScheme().toLowerCase(Locale.ROOT);
        if (!ALLOWED_SCHEMES.contains(scheme)) {
            throw unsafe("http 또는 https URL만 분석할 수 있습니다.");
        }

        if (parsed.getUserInfo() != null) {
            throw unsafe("계정 정보가 포함된 URL은 분석할 수 없습니다.");
        }

        String host = parsed.getHost();
        if (host == null || host.isBlank()) {
            throw unsafe("호스트 이름을 확인할 수 없습니다.");
        }

        String asciiHost;
        try {
            asciiHost = IDN.toASCII(host).toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException exception) {
            throw unsafe("호스트 이름이 올바르지 않습니다.");
        }

        rejectBlockedHost(asciiHost);
        rejectUnsafeIpLiteral(asciiHost);

        String path = parsed.getRawPath();
        if (path == null || path.isBlank()) {
            path = "/";
        }

        try {
            return new URI(
                    scheme,
                    null,
                    asciiHost,
                    parsed.getPort(),
                    path,
                    null,
                    null
            ).normalize();
        } catch (URISyntaxException exception) {
            throw unsafe("안전한 URL로 정규화할 수 없습니다.");
        }
    }

    private void rejectBlockedHost(String host) {
        if (BLOCKED_HOSTS.contains(host)
                || host.endsWith(".localhost")
                || host.endsWith(".local")
                || host.endsWith(".internal")) {
            throw unsafe("로컬 또는 내부 네트워크 주소는 분석할 수 없습니다.");
        }
    }

    private void rejectUnsafeIpLiteral(String host) {
        boolean ipv4 = IPV4_LITERAL.matcher(host).matches();
        boolean ipv6 = host.contains(":");
        if (!ipv4 && !ipv6) {
            return;
        }

        final InetAddress address;
        try {
            address = InetAddress.getByName(host);
        } catch (UnknownHostException exception) {
            throw unsafe("IP 주소를 확인할 수 없습니다.");
        }

        byte[] bytes = address.getAddress();
        boolean carrierGradeNat = bytes.length == 4
                && Byte.toUnsignedInt(bytes[0]) == 100
                && Byte.toUnsignedInt(bytes[1]) >= 64
                && Byte.toUnsignedInt(bytes[1]) <= 127;

        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()
                || carrierGradeNat) {
            throw unsafe("공개 인터넷 주소만 분석할 수 있습니다.");
        }
    }

    private ApiException unsafe(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "UNSAFE_URL", message);
    }
}

