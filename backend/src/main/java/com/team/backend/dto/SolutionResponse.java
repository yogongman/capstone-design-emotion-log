package com.team.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SolutionResponse {
    private Long solutionId;
    private String content;
}