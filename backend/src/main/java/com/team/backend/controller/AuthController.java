package com.team.backend.controller;

import com.team.backend.exception.UnauthorizedException;
import com.team.backend.security.JwtUtil;
import com.team.backend.annotation.LoginUser;
import com.team.backend.dto.GoogleLoginRequest;
import com.team.backend.dto.GoogleLoginResponse;
import com.team.backend.dto.SignupRequest;
import com.team.backend.entity.User;
import com.team.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 인증 관련 API 엔드포인트
 * - Google 소셜 로그인
 * - 회원가입
 * - 토큰 갱신
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    /**
     * 2.1. Google 소셜 로그인
     * POST /api/v1/auth/login/google
     *
     * 요청: { "token": "google_id_token..." }
     * 응답 (기존 회원): { "accessToken": "...", "refreshToken": "...", "isNewUser": false }
     * 응답 (신규 회원): { "accessToken": "temp_token...", "isNewUser": true }
     */
    @PostMapping("/login/google")
    public ResponseEntity<GoogleLoginResponse> loginWithGoogle(@RequestBody GoogleLoginRequest request) {
        GoogleLoginResponse response = authService.loginOrRegister(request.getToken());
        return ResponseEntity.ok(response);
    }

    /**
     * 2.2. 회원가입 (추가 정보 입력)
     * POST /api/v1/auth/signup
     *
     * 헤더: Authorization: Bearer {temp_token}
     * 요청: { "nickname": "...", "age": 25, "gender": "male" }
     * 응답: { "success": true, "accessToken": "real_token...", "refreshToken": "..." }
     */
    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(
            @RequestHeader("Authorization") String bearerToken,
            @RequestBody SignupRequest request
    ) {
        // LoginUser에서 추출된 user 객체를 이용하여 Google 로그인 정보 재구성
        // (임시 토큰의 userId == -1L 확인)

        // 실제로는 임시 토큰의 클레임에서 email, socialId를 가져야 함
        // 현재는 임시로 처리하고, STEP 4에서 필터에서 처리

        // 1. 토큰 껍질 벗기기
        String token = bearerToken.substring(7);

        // 2. 토큰 검증
        if (!jwtUtil.validateToken(token)) {
            throw new UnauthorizedException("유효하지 않은 토큰입니다.");
        }

        // 3. 토큰 안에 숨겨둔 이메일/소셜ID 꺼내기
        String email = jwtUtil.extractEmail(token);
        String socialId = jwtUtil.extractSocialId(token);
        Long tempUserId = jwtUtil.extractUserIdFromToken(token); // -1
        GoogleLoginResponse signupResponse = authService.signup(
                tempUserId,
                email,      // 토큰에서 나온 진짜 이메일
                socialId,   // 토큰에서 나온 진짜 소셜ID
                request.getNickname(),
                request.getAge(),
                request.getGender()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("accessToken", signupResponse.getAccessToken());
        response.put("refreshToken", signupResponse.getRefreshToken());

        return ResponseEntity.ok(response);
    }

    /**
     * 토큰 갱신 (선택사항)
     * POST /api/v1/auth/refresh
     *
     * 요청: { "refreshToken": "..." }
     * 응답: { "accessToken": "new_access_token..." }
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshAccessToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        String newAccessToken = authService.refreshAccessToken(refreshToken);

        Map<String, String> response = new HashMap<>();
        response.put("accessToken", newAccessToken);

        return ResponseEntity.ok(response);
    }
}
