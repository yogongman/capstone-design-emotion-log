package com.team.backend.config;

import com.team.backend.annotation.LoginUser;
import com.team.backend.entity.User;
import com.team.backend.exception.ResourceNotFoundException;
import com.team.backend.exception.UnauthorizedException;
import com.team.backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @LoginUser 어노테이션으로 현재 인증된 사용자를 주입
 *
 * 동작 방식:
 * - 개발 모드 (development): 설정된 테스트 User ID 반환 (기본값: 1)
 * - 프로덕션 모드 (production): JWT 토큰에서 추출한 userId로 사용자 조회
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final UserRepository userRepository;

    @Value("${auth.mode:development}")
    private String authMode;

    @Value("${auth.test-user-id:1}")
    private Long testUserId;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginUser.class)
                && User.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

        // 개발 모드: 테스트용 User ID 반환
        if ("development".equals(authMode)) {
            log.debug("Development mode: returning test user (ID: {})", testUserId);
            return userRepository.findById(testUserId)
                    .orElseThrow(() -> new RuntimeException(
                            "테스트용 유저(ID:" + testUserId + ")가 없습니다. SQL을 실행해주세요!"));
        }

        // 프로덕션 모드: JWT 토큰에서 userId 추출
        HttpServletRequest httpRequest = webRequest.getNativeRequest(HttpServletRequest.class);
        Long userId = (Long) httpRequest.getAttribute("userId");

        if (userId == null) {
            log.warn("No userId found in request attributes");
            throw new UnauthorizedException("인증이 필요합니다.");
        }

        log.debug("Production mode: resolving user (ID: {})", userId);
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    }
}