package com.dividendbot.news.service.engagement;

import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.util.Locale;
import java.util.Optional;

@Component
public class PublicUrlGuard {

    public Optional<URI> parse(String rawUrl) {
        try {
            if (rawUrl == null || rawUrl.isBlank()) return Optional.empty();
            URI uri = URI.create(rawUrl.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) return Optional.empty();
            if (!scheme.equalsIgnoreCase("https") && !scheme.equalsIgnoreCase("http")) return Optional.empty();
            if (uri.getUserInfo() != null) return Optional.empty();
            if (uri.getPort() != -1 && uri.getPort() != 80 && uri.getPort() != 443) return Optional.empty();

            String normalizedHost = host.toLowerCase(Locale.ROOT);
            if (normalizedHost.equals("localhost") || normalizedHost.endsWith(".localhost") || normalizedHost.endsWith(".local")) {
                return Optional.empty();
            }

            for (InetAddress address : InetAddress.getAllByName(normalizedHost)) {
                if (isPrivate(address)) return Optional.empty();
            }
            return Optional.of(uri);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private boolean isPrivate(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        return bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
    }
}
