package com.team.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SolutionRequest {
    // 1. 솔루션 생성 요청용
    private Long recordId;

    // 2. 피드백(평가) 요청용
    private Integer score; // 1~5점
}