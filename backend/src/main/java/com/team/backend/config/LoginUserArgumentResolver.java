// backend/src/main/java/com/team/backend/config/LoginUserArgumentResolver.java

package com.team.backend.config;

import com.team.backend.annotation.LoginUser;
import com.team.backend.entity.User;
import com.team.backend.exception.ResourceNotFoundException;
import com.team.backend.exception.UnauthorizedException;
import com.team.backend.repository.UserRepository;
import com.team.backend.security.JwtUtil;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil; // [필수] 토큰 해석을 위해 주입

    @Value("${auth.mode:development}")
    private String authMode;

    @Value("${auth.test-user-id:1}")
    private Long testUserId;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // @LoginUser 어노테이션이 붙어있고, 타입이 User 클래스인 경우만 동작
        return parameter.hasParameterAnnotation(LoginUser.class)
                && User.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

        // 1. [개발 모드] 무조건 테스트 유저(ID: 1) 반환
        // application.yaml에서 auth.mode가 development일 때 동작
        if ("development".equals(authMode)) {
            return userRepository.findById(testUserId)
                    .orElseThrow(() -> new RuntimeException("테스트용 유저(ID:" + testUserId + ")가 DB에 없습니다."));
        }

        // 2. [프로덕션 모드] 실제 토큰 기반 동작
        HttpServletRequest httpRequest = webRequest.getNativeRequest(HttpServletRequest.class);

        // JwtAuthenticationFilter에서 request에 담아준 정보 꺼내기
        Long userId = (Long) httpRequest.getAttribute("userId");
        String token = (String) httpRequest.getAttribute("token");

        if (userId == null) {
            // 필터를 거치지 않았거나 인증에 실패한 경우
            throw new UnauthorizedException("인증 정보가 없습니다. (userId is null)");
        }

        // ---------------------------------------------------------
        // [핵심 수정] 신규 가입자 (ID: -1) 처리 로직
        // ---------------------------------------------------------
        if (userId.equals(-1L)) {
            log.info("Resolving temporary user (Sign-up flow)");

            // 토큰이 없으면 정보를 꺼낼 수 없으므로 예외 처리
            if (token == null) {
                // 혹시 모를 상황 대비: 헤더에서 직접 꺼내기 시도
                String authHeader = httpRequest.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                } else {
                    throw new UnauthorizedException("임시 토큰이 없습니다.");
                }
            }

            // 토큰을 해독하여 '이메일'과 '소셜ID' 추출
            String email = jwtUtil.extractEmail(token);
            String socialId = jwtUtil.extractSocialId(token);

            // DB에 저장되지 않은 '임시 User 객체'를 생성하여 반환
            // (이 객체는 AuthController의 signup 메서드로 전달됨)
            return User.builder()
                    .id(-1L)       // 임시 ID
                    .email(email)  // 토큰에서 꺼낸 진짜 이메일
                    .socialId(socialId) // 토큰에서 꺼낸 진짜 소셜ID
                    .nickname("Guest")  // 임시 닉네임
                    .build();
        }

        // 3. [기존 유저] DB에서 조회
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다. (ID: " + userId + ")"));
    }
}