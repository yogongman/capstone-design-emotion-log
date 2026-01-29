package com.team.backend.exception;

/**
 * 입력값 검증 실패 시 발생하는 예외 (400)
 * - 필수 필드 누락
 * - 유효하지 않은 데이터 형식
 * - 비즈니스 규칙 위반
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
