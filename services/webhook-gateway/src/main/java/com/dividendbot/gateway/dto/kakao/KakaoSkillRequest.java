package com.dividendbot.gateway.dto.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.Map;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoSkillRequest {

    private Intent intent;
    private UserRequest userRequest;
    private Action action;

    public String getIntentName() {
        return intent != null ? intent.getName() : "unknown";
    }

    public String getUserId() {
        if (userRequest != null && userRequest.getUser() != null) {
            return userRequest.getUser().getId();
        }
        return "unknown";
    }

    public String getUtterance() {
        return userRequest != null ? userRequest.getUtterance() : "";
    }

    public Map<String, String> getParams() {
        return action != null ? action.getParams() : Map.of();
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Intent {
        private String name;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserRequest {
        private String utterance;
        private KakaoUser user;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KakaoUser {
        private String id;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Action {
        private Map<String, String> params;
    }
}
