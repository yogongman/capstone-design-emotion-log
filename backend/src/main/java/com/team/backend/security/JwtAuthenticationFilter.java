package com.team.backend.security;

import com.team.backend.entity.User; // User entity import
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Value("${auth.mode:development}")
    private String authMode;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. 개발 모드일 경우: 무조건 통과 (Test User ID: 1로 설정)
        if ("development".equals(authMode)) {
            // 개발 모드라도 SecurityContext에 인증 정보는 넣어줘야 함
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
            SecurityContextHolder.getContext().setAuthentication(auth);

            request.setAttribute("userId", 1L);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 2. 헤더에서 토큰 추출
            String token = resolveToken(request);

            // 3. 토큰 유효성 검사
            if (token != null && jwtUtil.validateToken(token)) {

                // 4. 토큰에서 사용자 ID 추출
                Long userId = jwtUtil.extractUserIdFromToken(token);

                // -----------------------------------------------------------
                // [핵심 수정 1] 스프링 시큐리티에게 "이 사람 인증됨!" 알리기
                // -----------------------------------------------------------
                // 비밀번호는 없으므로 null, 권한은 ROLE_USER로 임시 부여
                Authentication authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));

                // ★ 이 줄이 없으면 SecurityConfig가 "인증 안 됐잖아!" 하고 쫓아냅니다.
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // -----------------------------------------------------------
                // [핵심 수정 2] LoginUserArgumentResolver를 위해 정보 저장
                // -----------------------------------------------------------
                request.setAttribute("userId", userId);
                request.setAttribute("token", token);

                log.debug("Authentication set for userId: {}", userId);
            }
        } catch (Exception e) {
            log.error("Could not set user authentication in security context", e);
            // 여기서 예외를 던지지 않고 다음 필터로 넘겨야 SecurityConfig의 EntryPoint가 처리함
        }

        filterChain.doFilter(request, response);
    }

    // 헤더에서 "Bearer " 제거하고 토큰만 추출
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}