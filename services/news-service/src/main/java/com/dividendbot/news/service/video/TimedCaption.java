package com.dividendbot.news.service.video;

public record TimedCaption(double startSeconds, double endSeconds, String text) {
    public TimedCaption {
        startSeconds = Math.max(0, startSeconds);
        endSeconds = Math.max(startSeconds + 0.05, endSeconds);
        text = text == null ? "" : text.trim();
    }
}
