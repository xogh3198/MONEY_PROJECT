package com.dividendbot.news.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtProvider jwtProvider;

    public JwtAuthFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Only protect write operations on forum paths
        boolean isForumPath = path.startsWith("/api/forum/");
        boolean isWriteOperation = "POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method);

        if (!isForumPath || !isWriteOperation) {
            // For GET requests or non-forum paths, try to extract user info but don't block
            tryExtractUser(request);
            filterChain.doFilter(request, response);
            return;
        }

        // Write operations require authentication
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorized(response);
            return;
        }

        String token = authHeader.substring(7);
        if (!jwtProvider.validateToken(token)) {
            sendUnauthorized(response);
            return;
        }

        UUID userId = jwtProvider.getUserIdFromToken(token);
        String nickname = jwtProvider.getNicknameFromToken(token);
        request.setAttribute("userId", userId);
        request.setAttribute("nickname", nickname);

        filterChain.doFilter(request, response);
    }

    private void tryExtractUser(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtProvider.validateToken(token)) {
                request.setAttribute("userId", jwtProvider.getUserIdFromToken(token));
                request.setAttribute("nickname", jwtProvider.getNicknameFromToken(token));
            }
        }
    }

    private void sendUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\": \"인증이 필요합니다\"}");
    }
}
