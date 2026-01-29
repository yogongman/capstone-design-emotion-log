package com.team.backend.exception;

/**
 * 요청한 리소스를 찾을 수 없을 때 발생하는 예외 (404)
 * - 사용자 미존재
 * - 감정 기록 미존재
 * - 솔루션 미존재
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
