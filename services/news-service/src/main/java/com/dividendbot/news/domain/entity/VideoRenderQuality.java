package com.dividendbot.news.domain.entity;

public enum VideoRenderQuality {
    PREVIEW(540, 960, 30, 28),
    FINAL(1080, 1920, 30, 22);

    private final int width;
    private final int height;
    private final int fps;
    private final int crf;

    VideoRenderQuality(int width, int height, int fps, int crf) {
        this.width = width;
        this.height = height;
        this.fps = fps;
        this.crf = crf;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int fps() {
        return fps;
    }

    public int crf() {
        return crf;
    }
}
