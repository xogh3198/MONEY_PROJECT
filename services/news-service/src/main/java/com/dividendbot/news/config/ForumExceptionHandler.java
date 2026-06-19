package com.dividendbot.news.config;

import com.dividendbot.news.exception.ForumPostNotFoundException;
import com.dividendbot.news.exception.UnauthorizedAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class ForumExceptionHandler {

    @ExceptionHandler(ForumPostNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ForumPostNotFoundException ex) {
        return ResponseEntity.status(404).body(errorBody("게시글을 찾을 수 없습니다"));
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(UnauthorizedAccessException ex) {
        return ResponseEntity.status(403).body(errorBody("접근 권한이 없습니다"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + ", " + b)
                .orElse("입력값 오류");
        return ResponseEntity.badRequest().body(errorBody(msg));
    }

    private Map<String, Object> errorBody(String message) {
        return Map.of("error", message, "timestamp", LocalDateTime.now().toString());
    }
}
