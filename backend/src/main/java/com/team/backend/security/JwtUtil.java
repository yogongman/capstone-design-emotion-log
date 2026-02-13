package com.team.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    /**
     * AccessToken 생성 (15분 유효)
     */
    public String generateAccessToken(Long userId) {
        return buildToken(userId, accessTokenExpiration, "access");
    }

    /**
     * RefreshToken 생성 (7일 유효)
     */
    public String generateRefreshToken(Long userId) {
        return buildToken(userId, refreshTokenExpiration, "refresh");
    }

    /**
     * JWT 토큰 생성 (공통 로직)
     */
    private String buildToken(Long userId, long expirationMs, String tokenType) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("tokenType", tokenType)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 토큰에서 userId 추출
     * 토큰이 유효하지 않으면 예외 발생
     */
    public Long extractUserIdFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 토큰 유효성 검증
     * 유효하면 true, 무효하면 예외 throw
     */
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
            throw new JwtException("토큰이 만료되었습니다.");
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token is unsupported: {}", e.getMessage());
            throw new JwtException("지원하지 않는 토큰입니다.");
        } catch (MalformedJwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            throw new JwtException("유효하지 않은 토큰입니다.");
        } catch (SignatureException e) {
            log.warn("JWT signature validation failed: {}", e.getMessage());
            throw new JwtException("토큰 서명 검증에 실패했습니다.");
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
            throw new JwtException("토큰 정보가 비어있습니다.");
        }
    }

    /**
     * 토큰의 Claims 추출 (공통 로직)
     */
    private Claims getClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 토큰이 만료되었는지 확인
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 토큰 유형 확인 (access, refresh)
     */
    public String getTokenType(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.get("tokenType", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * [추가] 회원가입용 임시 토큰 생성 (email, socialId 포함)
     * 유효기간: 10분 (가입 정보를 입력하기에 충분한 시간)
     */
    public String generateSignupToken(String email, String socialId) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + (10 * 60 * 1000)); // 10분

        return Jwts.builder()
                .setSubject("signup-guest")
                .claim("userId", -1L)       // 아직 회원이 아님
                .claim("email", email)      // ★ 토큰에 이메일 저장
                .claim("socialId", socialId)// ★ 토큰에 소셜ID 저장
                .claim("tokenType", "signup")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * [추가] 토큰에서 이메일 추출
     */
    public String extractEmail(String token) {
        return getClaims(token).get("email", String.class);
    }

    /**
     * [추가] 토큰에서 SocialId 추출
     */
    public String extractSocialId(String token) {
        return getClaims(token).get("socialId", String.class);
    }
}
