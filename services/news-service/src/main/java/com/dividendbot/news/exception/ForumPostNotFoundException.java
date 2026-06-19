package com.dividendbot.news.exception;

import java.util.UUID;

public class ForumPostNotFoundException extends RuntimeException {
    public ForumPostNotFoundException(UUID id) {
        super("게시글을 찾을 수 없습니다: " + id);
    }
}
