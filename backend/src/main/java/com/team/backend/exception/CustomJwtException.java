package com.team.backend.exception;

/**
 * JWT 토큰 관련 예외
 * JwtUtil에서 throw되는 예외를 UnauthorizedException으로 변환하기 위한 중간 예외
 */
public class CustomJwtException extends RuntimeException {

    public CustomJwtException(String message) {
        super(message);
    }

    public CustomJwtException(String message, Throwable cause) {
        super(message, cause);
    }
}
