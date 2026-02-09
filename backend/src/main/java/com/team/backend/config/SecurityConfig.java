package com.team.backend.config;

import com.team.backend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

/**
 * Spring Security 설정
 * - JWT 필터 등록
 * - CORS 설정
 * - 인증 정책 설정
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${auth.mode:development}")
    private String authMode;

    /**
     * Spring Security FilterChain 설정
     * - CSRF 비활성화 (REST API는 상태 비저장)
     * - 세션 비활성화 (JWT 기반 인증)
     * - JWT 필터 등록
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (REST API)
                .csrf(csrf -> csrf.disable())

                // CORS 적용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 세션 비활성화 (JWT 기반)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 인증 정책 설정
                .authorizeHttpRequests(auth -> {
                    // 공개 경로
                    auth.requestMatchers("/api/v1/auth/**").permitAll()
                            .requestMatchers("/health").permitAll()
                            .requestMatchers("/actuator/**").permitAll();

                    // 개발 모드: 모든 요청 허용
                    if ("development".equals(authMode)) {
                        auth.anyRequest().permitAll();
                    } else {
                        // 프로덕션 모드: 나머지는 인증 필요
                        auth.anyRequest().authenticated();
                    }
                })

                // JWT 필터 등록 (UsernamePasswordAuthenticationFilter 전에)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS 설정
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 출처 (개발: localhost, 배포 시 실제 도메인으로 변경)
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:5173",  // Vite 기본 포트
                "http://localhost:8080",
                "http://127.0.0.1:3000",
                "http://127.0.0.1:5173"
        ));

        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // 허용할 헤더
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 인증 정보 포함 (쿠키, Authorization 헤더)
        configuration.setAllowCredentials(true);

        // Preflight 응답 캐시 시간 (초)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * 비밀번호 암호화 (선택사항)
     * JWT 기반 인증이므로 사용하지 않지만, 나중에 필요할 수 있음
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
