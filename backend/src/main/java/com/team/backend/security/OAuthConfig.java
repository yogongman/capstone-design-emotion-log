package com.team.backend.security;

import org.springframework.context.annotation.Configuration;

/**
 * Google OAuth2 설정
 * Google ID Token은 직접 REST API 호출로 검증함
 */
@Configuration
public class OAuthConfig {
    // REST API로 검증하므로 별도 Bean 불필요
}
