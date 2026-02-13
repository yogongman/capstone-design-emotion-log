package com.team.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {
    private String nickname; // 닉네임
    private Integer age;     // 나이
    private String gender;   // "male" or "female"
}
