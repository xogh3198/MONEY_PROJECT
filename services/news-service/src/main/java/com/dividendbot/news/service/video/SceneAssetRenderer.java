package com.dividendbot.news.service.video;

import com.dividendbot.news.dto.VideoRenderRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class SceneAssetRenderer {

    private static final int MAX_DOWNLOAD_BYTES = 15 * 1024 * 1024;

    private final ObjectMapper objectMapper;
    private final String pixabayApiKey;
    private final HttpClient httpClient;

    public SceneAssetRenderer(
            ObjectMapper objectMapper,
            @Value("${video.assets.pixabay-api-key:}") String pixabayApiKey
    ) {
        this.objectMapper = objectMapper;
        this.pixabayApiKey = pixabayApiKey;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    public RenderedSceneAsset render(
            VideoRenderRequest.Scene scene,
            int width,
            int height,
            Path outputFile
    ) {
        try {
            Files.createDirectories(outputFile.getParent());
            Optional<PixabayImage> remote = findPixabayImage(scene);
            BufferedImage source = remote.flatMap(this::downloadImage).orElse(null);
            BufferedImage canvas = createCanvas(source, scene.onScreenText(), width, height, scene.order());
            ImageIO.write(canvas, "png", outputFile.toFile());
            if (remote.isPresent() && source != null) {
                PixabayImage image = remote.get();
                return new RenderedSceneAsset(
                        outputFile,
                        "Pixabay / " + image.user() + " / " + image.pageUrl(),
                        "PIXABAY"
                );
            }
            return new RenderedSceneAsset(outputFile, "InvestBoard 자체 생성 카드", "GENERATED");
        } catch (Exception ignored) {
            try {
                BufferedImage fallback = createCanvas(null, scene.onScreenText(), width, height, scene.order());
                ImageIO.write(fallback, "png", outputFile.toFile());
                return new RenderedSceneAsset(outputFile, "InvestBoard 자체 생성 카드", "GENERATED");
            } catch (Exception e) {
                throw new IllegalStateException("영상 장면 이미지를 만들지 못했습니다.", e);
            }
        }
    }

    public boolean pixabayConfigured() {
        return pixabayApiKey != null && !pixabayApiKey.isBlank();
    }

    private Optional<PixabayImage> findPixabayImage(VideoRenderRequest.Scene scene) {
        if (!pixabayConfigured()) return Optional.empty();
        try {
            String query = searchQuery(scene);
            URI uri = URI.create(
                    "https://pixabay.com/api/?key=" + encode(pixabayApiKey)
                            + "&q=" + encode(query)
                            + "&image_type=photo&orientation=vertical&safesearch=true"
                            + "&lang=ko&per_page=3&order=popular"
            );
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(12))
                    .header("User-Agent", "InvestBoardVideo/1.0")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            if (response.statusCode() != 200) return Optional.empty();

            JsonNode hits = objectMapper.readTree(response.body()).path("hits");
            if (!hits.isArray() || hits.isEmpty()) return Optional.empty();
            JsonNode hit = hits.get(0);
            String imageUrl = firstNonBlank(
                    hit.path("largeImageURL").asText(""),
                    hit.path("webformatURL").asText("")
            );
            String pageUrl = hit.path("pageURL").asText("https://pixabay.com/");
            String user = hit.path("user").asText("unknown");
            if (imageUrl.isBlank() || !isPixabayHost(URI.create(imageUrl).getHost())) {
                return Optional.empty();
            }
            return Optional.of(new PixabayImage(URI.create(imageUrl), pageUrl, user));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private Optional<BufferedImage> downloadImage(PixabayImage image) {
        try {
            HttpRequest request = HttpRequest.newBuilder(image.imageUri())
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "InvestBoardVideo/1.0")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofByteArray()
            );
            String contentType = response.headers()
                    .firstValue("content-type")
                    .orElse("")
                    .toLowerCase(Locale.ROOT);
            if (response.statusCode() != 200
                    || response.body().length == 0
                    || response.body().length > MAX_DOWNLOAD_BYTES
                    || !contentType.startsWith("image/")) {
                return Optional.empty();
            }
            return Optional.ofNullable(ImageIO.read(new ByteArrayInputStream(response.body())));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private BufferedImage createCanvas(
            BufferedImage source,
            String headline,
            int width,
            int height,
            int sceneOrder
    ) {
        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = canvas.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        if (source == null) {
            paintGeneratedBackground(graphics, width, height, sceneOrder);
        } else {
            drawCover(graphics, source, width, height);
        }

        GradientPaint shade = new GradientPaint(
                0, height * 0.34f, new Color(5, 10, 22, 15),
                0, height, new Color(5, 10, 22, 245)
        );
        graphics.setPaint(shade);
        graphics.fillRect(0, 0, width, height);

        int side = Math.max(34, width / 14);
        int chipHeight = Math.max(44, height / 22);
        graphics.setColor(new Color(27, 213, 165));
        graphics.fill(new RoundRectangle2D.Double(
                side, side, width * 0.34, chipHeight, chipHeight, chipHeight
        ));
        graphics.setColor(new Color(3, 18, 25));
        graphics.setFont(new Font("SansSerif", Font.BOLD, Math.max(18, width / 27)));
        graphics.drawString("INVESTBOARD  ·  핵심 1분", side + 18, side + chipHeight * 2 / 3);

        int fontSize = Math.max(38, width / 11);
        graphics.setFont(new Font("SansSerif", Font.BOLD, fontSize));
        List<String> lines = wrap(graphics, normalize(headline), width - side * 2, 3);
        int lineHeight = Math.round(fontSize * 1.28f);
        int startY = height - Math.max(height / 4, lines.size() * lineHeight + height / 12);
        graphics.setColor(new Color(255, 255, 255));
        for (String line : lines) {
            graphics.drawString(line, side, startY);
            startY += lineHeight;
        }

        graphics.setFont(new Font("SansSerif", Font.PLAIN, Math.max(18, width / 29)));
        graphics.setColor(new Color(215, 225, 238));
        graphics.drawString("사실 확인 후 공개 · 투자 판단은 본인 책임", side, height - side);
        graphics.dispose();
        return canvas;
    }

    private void paintGeneratedBackground(Graphics2D graphics, int width, int height, int sceneOrder) {
        Color[] colors = {
                new Color(10, 35, 66),
                new Color(26, 39, 72),
                new Color(10, 57, 62),
                new Color(54, 31, 73)
        };
        Color start = colors[Math.floorMod(sceneOrder, colors.length)];
        graphics.setPaint(new GradientPaint(0, 0, start, width, height, new Color(4, 12, 25)));
        graphics.fillRect(0, 0, width, height);

        graphics.setColor(new Color(50, 235, 183, 55));
        for (int index = 0; index < 8; index++) {
            int diameter = width / 3 + index * width / 30;
            graphics.fillOval(
                    width - diameter / 2 - index * width / 15,
                    height / 8 + index * height / 12,
                    diameter,
                    diameter
            );
        }
    }

    private void drawCover(Graphics2D graphics, BufferedImage source, int width, int height) {
        double scale = Math.max(
                width / (double) source.getWidth(),
                height / (double) source.getHeight()
        );
        int drawWidth = (int) Math.ceil(source.getWidth() * scale);
        int drawHeight = (int) Math.ceil(source.getHeight() * scale);
        int x = (width - drawWidth) / 2;
        int y = (height - drawHeight) / 2;
        graphics.drawImage(source, x, y, drawWidth, drawHeight, null);
    }

    private List<String> wrap(Graphics2D graphics, String value, int maxWidth, int maxLines) {
        if (value.isBlank()) return List.of("오늘의 핵심 이슈");
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : value.split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && graphics.getFontMetrics().stringWidth(candidate) > maxWidth) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (!line.isEmpty()) line.append(' ');
            line.append(word);
            if (lines.size() == maxLines - 1) break;
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }

    private String searchQuery(VideoRenderRequest.Scene scene) {
        if (scene.visualSearchTerms() != null) {
            String query = scene.visualSearchTerms().stream()
                    .filter(term -> term != null && !term.isBlank())
                    .limit(3)
                    .reduce((left, right) -> left + " " + right)
                    .orElse("");
            if (!query.isBlank()) return query;
        }
        return normalize(scene.onScreenText());
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value.replaceAll("<[^>]+>", " ")
                .replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean isPixabayHost(String host) {
        if (host == null) return false;
        String normalized = host.toLowerCase(Locale.ROOT);
        return normalized.equals("pixabay.com")
                || normalized.endsWith(".pixabay.com")
                || normalized.equals("pixabaycdn.com")
                || normalized.endsWith(".pixabaycdn.com");
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second == null ? "" : second;
    }

    private record PixabayImage(URI imageUri, String pageUrl, String user) {
    }
}
