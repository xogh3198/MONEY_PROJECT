package com.dividendbot.gateway.dto.kakao;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KakaoSkillResponse {

    @Builder.Default
    private String version = "2.0";
    private Template template;

    @Getter
    @Builder
    public static class Template {
        private List<Output> outputs;
        private List<QuickReply> quickReplies;
    }

    @Getter
    @Builder
    public static class Output {
        private SimpleText simpleText;
    }

    @Getter
    @Builder
    public static class SimpleText {
        private String text;
    }

    @Getter
    @Builder
    public static class QuickReply {
        private String label;
        @Builder.Default
        private String action = "message";
        private String messageText;
    }

    /**
     * 단순 텍스트 응답 생성 헬퍼
     */
    public static KakaoSkillResponse simpleText(String text) {
        return KakaoSkillResponse.builder()
                .template(Template.builder()
                        .outputs(List.of(
                                Output.builder()
                                        .simpleText(SimpleText.builder().text(text).build())
                                        .build()))
                        .build())
                .build();
    }

    /**
     * 텍스트 + 퀵리플라이 응답 생성 헬퍼
     */
    public static KakaoSkillResponse withQuickReplies(String text, List<QuickReply> quickReplies) {
        return KakaoSkillResponse.builder()
                .template(Template.builder()
                        .outputs(List.of(
                                Output.builder()
                                        .simpleText(SimpleText.builder().text(text).build())
                                        .build()))
                        .quickReplies(quickReplies)
                        .build())
                .build();
    }
}
