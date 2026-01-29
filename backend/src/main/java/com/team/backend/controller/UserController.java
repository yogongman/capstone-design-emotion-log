package com.team.backend.controller;

import com.team.backend.annotation.LoginUser;
import com.team.backend.dto.UserInfoResponse;
import com.team.backend.entity.User;
import com.team.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 정보 관련 API 엔드포인트
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 2.3. 내 정보 조회 (앱 실행 시 필수)
     * GET /api/v1/users/me
     *
     * 헤더: Authorization: Bearer {accessToken}
     * 응답: { "id": 1, "nickname": "...", "age": 25, "email": "..." }
     */
    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getMyInfo(@LoginUser User user) {
        UserInfoResponse response = userService.getUserInfo(user.getId());
        return ResponseEntity.ok(response);
    }
}
