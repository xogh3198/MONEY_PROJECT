package com.dividendbot.news.promotion.channel;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ChannelCatalog {

    private final List<Channel> channels = List.of(
            new Channel(
                    "naver-search",
                    "네이버 검색",
                    java.util.Set.of("상담 문의", "방문", "구매"),
                    "수요 포착",
                    "검색형 랜딩·키워드",
                    "검색 의도가 분명한 고객의 질문에 답하는 페이지를 만드세요.",
                    76,
                    100_000,
                    500_000,
                    4,
                    "보통",
                    "https://ads.naver.com/help/faq/1348",
                    "2026-07-26",
                    false
            ),
            new Channel(
                    "naver-blog",
                    "네이버 블로그",
                    java.util.Set.of("상담 문의", "방문", "인지도"),
                    "검색·신뢰",
                    "문제 해결형 글",
                    "고객이 결정을 미루는 이유를 사례와 체크리스트로 설명하세요.",
                    72,
                    0,
                    150_000,
                    5,
                    "보통",
                    "https://section.blog.naver.com/",
                    "2026-07-26",
                    true
            ),
            new Channel(
                    "local-community",
                    "지역 커뮤니티",
                    java.util.Set.of("상담 문의", "방문", "인지도"),
                    "지역 발견",
                    "정보형 소개글",
                    "광고 문구보다 지역 고객에게 유용한 정보와 실제 위치를 먼저 제시하세요.",
                    70,
                    0,
                    100_000,
                    3,
                    "낮음",
                    "https://business.kakao.com/",
                    "2026-07-26",
                    true
            ),
            new Channel(
                    "instagram-reels",
                    "인스타그램 릴스",
                    java.util.Set.of("인지도", "방문", "상담 문의"),
                    "발견·관심",
                    "30초 질문 답변",
                    "고객이 가장 자주 묻는 질문 하나를 30초 안에 답하세요.",
                    68,
                    0,
                    400_000,
                    6,
                    "낮음",
                    "https://www.facebook.com/business/help",
                    "2026-07-26",
                    true
            ),
            new Channel(
                    "youtube-shorts",
                    "유튜브 쇼츠",
                    java.util.Set.of("인지도", "가입", "상담 문의"),
                    "발견·신뢰",
                    "30초 설명 영상",
                    "하나의 오해를 바로잡고 사이트의 근거 페이지로 연결하세요.",
                    65,
                    0,
                    500_000,
                    7,
                    "낮음",
                    "https://support.google.com/youtube/",
                    "2026-07-26",
                    true
            ),
            new Channel(
                    "google-search",
                    "Google 검색광고",
                    java.util.Set.of("구매", "가입", "상담 문의"),
                    "수요 포착",
                    "검색광고",
                    "전환 페이지와 측정이 준비된 뒤 소액 키워드 실험을 시작하세요.",
                    64,
                    150_000,
                    700_000,
                    4,
                    "보통",
                    "https://support.google.com/google-ads/answer/10486536?hl=ko",
                    "2026-07-26",
                    false
            ),
            new Channel(
                    "launch-community",
                    "제품 출시 커뮤니티",
                    java.util.Set.of("가입", "인지도"),
                    "초기 검증",
                    "빌드 로그·출시글",
                    "만든 이유, 누구의 문제인지, 무엇을 배우고 싶은지 솔직하게 공유하세요.",
                    62,
                    0,
                    100_000,
                    4,
                    "보통",
                    "https://disquiet.io/",
                    "2026-07-26",
                    true
            )
    );

    public List<Channel> all() {
        return channels;
    }

    public Optional<Channel> findById(String id) {
        return channels.stream().filter(channel -> channel.id().equals(id)).findFirst();
    }
}

