package com.team.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleLoginResponse {
    private String accessToken;
    private String refreshToken;  // 신규 회원은 null
    private Boolean isNewUser;
    private String email;         // 신규 회원가입용 (로그인 응답에는 null)
    private String socialId;      // 신규 회원가입용 (로그인 응답에는 null)
}
