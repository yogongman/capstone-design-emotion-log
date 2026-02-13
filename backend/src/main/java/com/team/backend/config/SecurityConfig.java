package com.team.backend.config;

import com.team.backend.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse; // [Import 추가]
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // [Log 추가]
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${auth.mode:development}")
    private String authMode;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화
                .csrf(csrf -> csrf.disable())

                // CORS 적용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 세션 비활성화
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // [중요] 인증 정책 설정
                .authorizeHttpRequests(auth -> {
                    // 1. 공개 경로 (로그인, 헬스체크)
                    auth.requestMatchers("/api/v1/auth/**", "/health", "/actuator/**", "/error").permitAll();

                    // 2. OPTIONS 요청 (Preflight) 허용
                    auth.requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll();

                    // 3. 나머지 요청 처리
                    if ("development".equals(authMode)) {
                        auth.anyRequest().permitAll(); // 개발 모드: 모두 허용
                    } else {
                        auth.anyRequest().authenticated(); // 프로덕션: 인증 필수
                    }
                })

                // [추가됨] ★ 인증 실패 시 401/403 에러를 JSON으로 응답 (이게 없으면 빈 화면이 뜸)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            log.warn("인증 실패 - URL: {}, Error: {}", request.getRequestURI(), authException.getMessage());

                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
                            response.getWriter().write("{\"success\": false, \"error\": \"UNAUTHORIZED\", \"message\": \"인증이 필요하거나 토큰이 유효하지 않습니다.\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            log.warn("권한 실패 - URL: {}, Error: {}", request.getRequestURI(), accessDeniedException.getMessage());

                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403
                            response.getWriter().write("{\"success\": false, \"error\": \"FORBIDDEN\", \"message\": \"접근 권한이 없습니다.\"}");
                        })
                )

                // JWT 필터 등록
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 개발 환경 출처 허용
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://localhost:8080",
                "http://127.0.0.1:3000",
                "http://127.0.0.1:5173",
                "http://127.0.0.1:5500", // Live Server
                "http://localhost:5500"
        ));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}