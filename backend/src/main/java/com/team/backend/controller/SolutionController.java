package com.team.backend.controller;

import com.team.backend.annotation.LoginUser;
import com.team.backend.dto.SolutionRequest;
import com.team.backend.dto.SolutionResponse;
import com.team.backend.entity.User;
import com.team.backend.service.SolutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/solutions")
@RequiredArgsConstructor
public class SolutionController {

    private final SolutionService solutionService;

    // 5.1 솔루션 생성
    @PostMapping("/generate")
    public ResponseEntity<SolutionResponse> generateSolution(
            @LoginUser User user,
            @RequestBody SolutionRequest request
    ) {
        SolutionResponse response = solutionService.generateSolution(user, request.getRecordId());
        return ResponseEntity.ok(response);
    }

    // 5.2 솔루션 평가
    @PostMapping("/{solutionId}/feedback")
    public ResponseEntity<Map<String, Boolean>> evaluateSolution(
            @LoginUser User user,
            @PathVariable Long solutionId,
            @RequestBody SolutionRequest request
    ) {
        solutionService.evaluateSolution(user, solutionId, request.getScore());
        return ResponseEntity.ok(Map.of("success", true));
    }
}