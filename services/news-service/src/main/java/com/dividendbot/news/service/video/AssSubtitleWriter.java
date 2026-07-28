package com.dividendbot.news.service.video;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

@Component
public class AssSubtitleWriter {

    public Path write(List<TimedCaption> captions, int width, int height, Path outputFile) {
        try {
            StringBuilder ass = new StringBuilder();
            ass.append("[Script Info]\n")
                    .append("ScriptType: v4.00+\n")
                    .append("PlayResX: ").append(width).append('\n')
                    .append("PlayResY: ").append(height).append('\n')
                    .append("WrapStyle: 2\n")
                    .append("ScaledBorderAndShadow: yes\n\n")
                    .append("[V4+ Styles]\n")
                    .append("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, ")
                    .append("OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ")
                    .append("ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, ")
                    .append("Alignment, MarginL, MarginR, MarginV, Encoding\n")
                    .append("Style: Main,Noto Sans CJK KR,")
                    .append(Math.max(34, width / 13))
                    .append(",&H00FFFFFF,&H0000FFFF,&H00101824,&H90000000,-1,0,0,0,")
                    .append("100,100,0,0,1,4,1,2,")
                    .append(width / 14).append(',').append(width / 14).append(',').append(height / 7)
                    .append(",1\n\n")
                    .append("[Events]\n")
                    .append("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n");

            for (TimedCaption caption : captions) {
                ass.append("Dialogue: 0,")
                        .append(formatTime(caption.startSeconds())).append(',')
                        .append(formatTime(caption.endSeconds())).append(',')
                        .append("Main,,0,0,0,,")
                        .append(escape(caption.text()))
                        .append('\n');
            }
            Files.writeString(outputFile, ass, StandardCharsets.UTF_8);
            return outputFile;
        } catch (Exception e) {
            throw new IllegalStateException("자막 파일을 만들지 못했습니다.", e);
        }
    }

    private String formatTime(double seconds) {
        long centiseconds = Math.max(0, Math.round(seconds * 100));
        long hours = centiseconds / 360000;
        long minutes = (centiseconds / 6000) % 60;
        long wholeSeconds = (centiseconds / 100) % 60;
        long fraction = centiseconds % 100;
        return String.format(Locale.ROOT, "%d:%02d:%02d.%02d", hours, minutes, wholeSeconds, fraction);
    }

    private String escape(String text) {
        return text.replace("\\", "\\\\")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("\n", "\\N")
                .replace("\r", "");
    }
}
