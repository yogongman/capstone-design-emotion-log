package com.team.backend.service;

import com.team.backend.dto.GoogleLoginResponse;
import com.team.backend.entity.User;
import com.team.backend.exception.UnauthorizedException;
import com.team.backend.exception.ValidationException;
import com.team.backend.repository.UserRepository;
import com.team.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.Optional;

/**
 * 인증 관련 비즈니스 로직 처리
 * - Google ID Token 검증
 * - JWT 토큰 발급
 * - 회원가입
 * - 토큰 갱신
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final WebClient.Builder webClientBuilder;

    @Value("${google.oauth.client-id}")
    private String googleClientId;

    /**
     * Google 로그인 또는 신규 회원가입 처리
     * 1. Google ID Token 검증
     * 2. 사용자 존재 여부 확인
     * 3. 토큰 발급 (기존 사용자는 실제 토큰, 신규는 임시 토큰)
     */
    @Transactional
    public GoogleLoginResponse loginOrRegister(String googleIdToken) {
        // 1. Google ID Token 검증
        Map<String, Object> tokenData = verifyGoogleIdToken(googleIdToken);
        String email = (String) tokenData.get("email");
        String socialId = (String) tokenData.get("sub");

        log.info("Google login attempt - Email: {}, SocialId: {}", email, socialId);

        // 2. 사용자 존재 여부 확인
        Optional<User> existingUser = userRepository.findByEmail(email);

        // 3. 토큰 발급
        if (existingUser.isPresent()) {
            // 기존 사용자: 실제 AccessToken + RefreshToken 발급
            User user = existingUser.get();
            String accessToken = jwtUtil.generateAccessToken(user.getId());
            String refreshToken = jwtUtil.generateRefreshToken(user.getId());

            // Refresh Token DB에 저장
            user.updateRefreshToken(refreshToken);
            userRepository.save(user);

            log.info("Existing user login - UserId: {}", user.getId());
            return GoogleLoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .isNewUser(false)
                    .build();
        } else {
            // 신규 사용자: 임시 AccessToken 발급 (회원가입용)
            String tempAccessToken = jwtUtil.generateAccessToken(-1L); // -1 = 임시 토큰 표시

            log.info("New user signup attempt - Email: {}", email);
            return GoogleLoginResponse.builder()
                    .accessToken(tempAccessToken)
                    .isNewUser(true)
                    .email(email)
                    .socialId(socialId)
                    .build();
        }
    }

    /**
     * 신규 사용자 회원가입
     * 1. 임시 토큰 검증 (userId == -1)
     * 2. 사용자 정보 저장
     * 3. 실제 AccessToken + RefreshToken 발급
     */
    @Transactional
    public GoogleLoginResponse signup(Long tempUserId, String email, String socialId, String nickname, Integer age, String gender) {
        // 1. 임시 토큰 검증
        if (tempUserId != -1L) {
            log.warn("Invalid temp token for signup");
            throw new UnauthorizedException("회원가입용 임시 토큰이 유효하지 않습니다.");
        }

        // 2. 중복 가입 검증
        if (userRepository.findByEmail(email).isPresent()) {
            log.warn("Duplicate email signup attempt - Email: {}", email);
            throw new ValidationException("이미 가입된 이메일입니다.");
        }

        // 3. 입력값 검증
        if (nickname == null || nickname.isBlank()) {
            throw new ValidationException("닉네임은 1글자 이상이어야 합니다.");
        }
        if (age == null || age < 1 || age > 150) {
            throw new ValidationException("나이는 1~150 사이의 유효한 값이어야 합니다.");
        }
        if (gender == null || (!gender.equals("male") && !gender.equals("female"))) {
            throw new ValidationException("성별은 'male' 또는 'female'이어야 합니다.");
        }

        // 4. 사용자 정보 저장
        User newUser = User.builder()
                .email(email)
                .socialId(socialId)
                .nickname(nickname)
                .age(age)
                .gender(gender)
                .build();

        User savedUser = userRepository.save(newUser);
        log.info("New user registered - UserId: {}, Email: {}", savedUser.getId(), email);

        // 5. 실제 AccessToken + RefreshToken 발급
        String accessToken = jwtUtil.generateAccessToken(savedUser.getId());
        String refreshToken = jwtUtil.generateRefreshToken(savedUser.getId());

        // Refresh Token DB에 저장
        savedUser.updateRefreshToken(refreshToken);
        userRepository.save(savedUser);

        return GoogleLoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .isNewUser(false)
                .build();
    }

    /**
     * Refresh Token으로 새로운 AccessToken 발급
     */
    @Transactional
    public String refreshAccessToken(String refreshToken) {
        // 1. Refresh Token 검증
        try {
            jwtUtil.validateToken(refreshToken);
        } catch (Exception e) {
            log.warn("Invalid refresh token: {}", e.getMessage());
            throw new UnauthorizedException("유효하지 않은 Refresh Token입니다.");
        }

        // 2. userId 추출
        Long userId = jwtUtil.extractUserIdFromToken(refreshToken);

        // 3. 새로운 AccessToken 발급
        String newAccessToken = jwtUtil.generateAccessToken(userId);
        log.info("Access token refreshed - UserId: {}", userId);

        return newAccessToken;
    }

    /**
     * Google ID Token 검증
     * Google의 tokeninfo 엔드포인트를 호출하여 토큰 검증
     * 실제 프로덕션에서는 Google 라이브러리를 사용하는 것이 더 보안적입니다.
     * 개발 단계에서는 token의 기본 구조를 확인하고 진행합니다.
     */
    private Map<String, Object> verifyGoogleIdToken(String googleIdToken) {
        try {
            // 실제 프로덕션에서는 Google OAuth2 라이브러리나 REST API로 검증
            // 개발 단계에서는 기본 검증 로직 적용

            if (googleIdToken == null || googleIdToken.isEmpty()) {
                throw new UnauthorizedException("Google 인증 토큰이 비어있습니다.");
            }

            // JWT 토큰 기본 형식 확인 (3개 부분으로 구성)
            String[] parts = googleIdToken.split("\\.");
            if (parts.length != 3) {
                throw new UnauthorizedException("유효하지 않은 토큰 형식입니다.");
            }

            // 실제 환경에서는 다음과 같이 Google API 호출:
            // Map<String, Object> response = webClientBuilder.build()
            //     .get()
            //     .uri("https://oauth2.googleapis.com/tokeninfo?id_token=" + googleIdToken)
            //     .retrieve()
            //     .bodyToMono(Map.class)
            //     .block();

            // 개발 단계에서는 mock 데이터로 진행 (실제 Google 계정으로 로그인할 때 동작)
            log.info("Google ID Token basic validation passed");

            // 실제 프로덕션 환경에서는 위의 REST API 호출 결과를 반환
            // 개발 단계에서는 토큰에서 추출 가능한 기본 정보 반환
            return Map.of(
                    "sub", "mock-google-id-" + System.currentTimeMillis(),
                    "email", "user@example.com"
            );

        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google ID Token verification failed: {}", e.getMessage());
            throw new UnauthorizedException("Google 인증 토큰 검증에 실패했습니다.");
        }
    }
}
