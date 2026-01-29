package com.team.backend.exception;

/**
 * 인증 실패 시 발생하는 예외 (401)
 * - JWT 토큰이 없거나 유효하지 않음
 * - 만료된 토큰
 * - 서명 검증 실패
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
