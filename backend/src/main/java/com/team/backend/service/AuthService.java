package com.team.backend.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

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

import java.util.Collections;
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

    @Value("${google.oauth.client-id}")
    private String googleClientId;

    @Transactional
    public GoogleLoginResponse loginOrRegister(String googleIdToken) {
        // 1. 구글 라이브러리로 토큰 검증 및 정보 추출
        Payload payload = verifyGoogleIdToken(googleIdToken);

        String email = payload.getEmail();
        String socialId = payload.getSubject(); // 구글의 유니크 ID (sub)

        // 2. 가입 여부 확인 (이하 로직은 기존과 동일)
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            String accessToken = jwtUtil.generateAccessToken(user.getId());
            String refreshToken = jwtUtil.generateRefreshToken(user.getId());
            user.updateRefreshToken(refreshToken);

            return GoogleLoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .isNewUser(false)
                    .build();
        } else {
            // 신규 회원: 토큰에 정보 담아서 임시 발급
            String signupToken = jwtUtil.generateSignupToken(email, socialId);

            return GoogleLoginResponse.builder()
                    .accessToken(signupToken)
                    .isNewUser(true)
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
    private Payload verifyGoogleIdToken(String tokenString) {
        try {
            // 1. 검증기(Verifier) 생성
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId)) // 내 Client ID가 맞는지 확인
                    .build();

            // 2. 검증 수행
            GoogleIdToken idToken = verifier.verify(tokenString);

            if (idToken != null) {
                // 검증 성공 시 Payload 반환
                return idToken.getPayload();
            } else {
                log.warn("Invalid ID token.");
                throw new UnauthorizedException("유효하지 않은 구글 토큰입니다.");
            }

        } catch (Exception e) {
            log.error("Google ID Token verification failed", e);
            throw new UnauthorizedException("구글 토큰 검증 실패: " + e.getMessage());
        }
    }
}