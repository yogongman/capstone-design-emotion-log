package com.team.backend.security;

import com.team.backend.exception.UnauthorizedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 토큰 인증 필터
 * 모든 요청에서 Authorization 헤더의 JWT 토큰을 검증
 * 검증된 사용자 정보를 요청 속성에 저장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    // 인증이 필요 없는 경로 (공개 API)
    private static final String[] PUBLIC_PATHS = {
            "/api/v1/auth/login/google",
            "/api/v1/auth/signup",
            "/api/v1/auth/refresh",
            "/health",
            "/actuator"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        String method = request.getMethod();

        // 공개 경로는 토큰 검증 스킵
        if (isPublicPath(requestPath)) {
            log.debug("Public path accessed: {} {}", method, requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 1. Authorization 헤더에서 토큰 추출
            String token = extractToken(request);

            if (token == null) {
                log.warn("No token provided for protected path: {} {}", method, requestPath);
                throw new UnauthorizedException("인증 토큰이 필요합니다.");
            }

            // 2. 토큰 검증
            jwtUtil.validateToken(token);

            // 3. userId 추출
            Long userId = jwtUtil.extractUserIdFromToken(token);

            // 4. 요청 속성에 userId 저장 (Controller에서 @LoginUser로 사용)
            request.setAttribute("userId", userId);
            request.setAttribute("token", token);

            log.debug("Token validated for user: {}", userId);
            filterChain.doFilter(request, response);

        } catch (UnauthorizedException e) {
            log.warn("Authentication failed: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\": false, \"error\": \"UNAUTHORIZED\", \"message\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            log.error("Unexpected error in authentication filter", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\": false, \"error\": \"INTERNAL_SERVER_ERROR\", \"message\": \"인증 처리 중 오류가 발생했습니다.\"}");
        }
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 추출
     * 형식: "Bearer {token}"
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer " 제거
        }

        return null;
    }

    /**
     * 공개 경로인지 확인
     */
    private boolean isPublicPath(String requestPath) {
        for (String publicPath : PUBLIC_PATHS) {
            if (requestPath.startsWith(publicPath)) {
                return true;
            }
        }
        return false;
    }
}
