package com.team.backend.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * HTTP 요청/응답 로깅 필터
 * 모든 요청과 응답을 로깅하여 디버깅 용이
 */
@Slf4j
@Component
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        // 요청 본문 캐싱 (여러 번 읽기 위함)
        ContentCachingRequestWrapper cachedRequest = new ContentCachingRequestWrapper(request);

        // 응답 본문 캐싱 (여러 번 읽기 위함)
        ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(response);

        try {
            // 1. 요청 정보 로깅
            logRequest(cachedRequest);

            // 2. 필터 체인 실행
            filterChain.doFilter(cachedRequest, cachedResponse);

            // 3. 응답 정보 로깅
            long duration = System.currentTimeMillis() - startTime;
            logResponse(cachedRequest, cachedResponse, duration);

        } finally {
            // 응답 본문을 클라이언트에 전송
            cachedResponse.copyBodyToResponse();
        }
    }

    /**
     * 요청 정보 로깅
     */
    private void logRequest(ContentCachingRequestWrapper request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String contentType = request.getContentType();

        StringBuilder log = new StringBuilder();
        log.append("\n");
        log.append("===========================================\n");
        log.append("[REQUEST] ").append(method).append(" ").append(uri);

        if (queryString != null) {
            log.append("?").append(queryString);
        }
        log.append("\n");

        // 요청 헤더 로깅
        log.append("Headers:\n");
        request.getHeaderNames().asIterator().forEachRemaining(name -> {
            String value = request.getHeader(name);
            // Authorization 헤더는 마스킹 (보안)
            if ("authorization".equalsIgnoreCase(name)) {
                value = value.substring(0, Math.min(20, value.length())) + "...";
            }
            log.append("  ").append(name).append(": ").append(value).append("\n");
        });

        // 요청 본문 로깅 (Content-Type이 application/json일 때만)
        if (contentType != null && contentType.contains("application/json")) {
            byte[] content = request.getContentAsByteArray();
            if (content.length > 0) {
                String body = new String(content, StandardCharsets.UTF_8);
                log.append("Body: ").append(body).append("\n");
            }
        }

        log.append("===========================================");
        logger.info(log.toString());
    }

    /**
     * 응답 정보 로깅
     */
    private void logResponse(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response, long duration) {
        int status = response.getStatus();
        String contentType = response.getContentType();

        StringBuilder log = new StringBuilder();
        log.append("\n");
        log.append("===========================================\n");
        log.append("[RESPONSE] ").append(status).append(" ");

        // HTTP 상태 코드 설명
        if (status >= 400) {
            log.append("ERROR");
        } else if (status >= 300) {
            log.append("REDIRECT");
        } else {
            log.append("OK");
        }
        log.append(" (").append(duration).append("ms)\n");

        // 응답 헤더 로깅
        log.append("Headers:\n");
        response.getHeaderNames().forEach(name -> {
            String value = response.getHeader(name);
            log.append("  ").append(name).append(": ").append(value).append("\n");
        });

        // 응답 본문 로깅 (Content-Type이 application/json일 때만)
        if (contentType != null && contentType.contains("application/json")) {
            byte[] content = response.getContentAsByteArray();
            if (content.length > 0) {
                String body = new String(content, StandardCharsets.UTF_8);
                log.append("Body: ").append(body).append("\n");
            }
        }

        log.append("===========================================");

        // 상태 코드에 따라 로그 레벨 결정
        if (status >= 500) {
            logger.error(log.toString());
        } else if (status >= 400) {
            logger.warn(log.toString());
        } else {
            logger.info(log.toString());
        }
    }
}
