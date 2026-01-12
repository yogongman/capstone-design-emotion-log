package com.team.backend.controller;

import com.team.backend.annotation.LoginUser;
import com.team.backend.dto.EmotionRecordRequest;
import com.team.backend.dto.EmotionRecordResponse; // [추가]
import com.team.backend.entity.User;
import com.team.backend.service.DiaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List; // [추가]
import java.util.Map;

@RestController
@RequestMapping("/api/v1/records")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    // 1. 저장
    @PostMapping
    public ResponseEntity<Map<String, Object>> createDiary(@LoginUser User user, @RequestBody EmotionRecordRequest request) {
        Long diaryId = diaryService.saveDiary(user, request);

        // 명세서: { "id": 101, "message": "저장되었습니다." }
        Map<String, Object> response = Map.of(
                "id", diaryId,
                "message", "저장되었습니다."
        );
        return ResponseEntity.ok(response);
    }

    // 2. 수정
    @PatchMapping("/{recordId}")
    public ResponseEntity<Map<String, Boolean>> updateDiary(
            @LoginUser User user,
            @PathVariable Long recordId,
            @RequestBody EmotionRecordRequest request
    ) {
        diaryService.updateDiary(user, recordId, request);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // 3. 삭제
    @DeleteMapping("/{recordId}")
    public ResponseEntity<Map<String, Boolean>> deleteDiary(
            @LoginUser User user,
            @PathVariable Long recordId
    ) {
        diaryService.deleteDiary(user, recordId);
        return ResponseEntity.ok(Map.of("success", true));
    }
    // 월간 기록
    @GetMapping("/monthly")
    public ResponseEntity<List<EmotionRecordResponse>> getMonthlyRecords(
            @LoginUser User user,
            @RequestParam int year,   // URL 쿼리 파라미터 (?year=2025)
            @RequestParam int month   // URL 쿼리 파라미터 (&month=11)
    ) {
        List<EmotionRecordResponse> responses = diaryService.getMonthlyRecords(user, year, month);
        return ResponseEntity.ok(responses);
    }

    // 일간 기록
    @GetMapping("/daily")
    public ResponseEntity<List<EmotionRecordResponse>> getDailyRecords(
            @LoginUser User user,
            @RequestParam String date // ?date=2025-11-27
    ) {
        List<EmotionRecordResponse> responses = diaryService.getDailyRecords(user, date);
        return ResponseEntity.ok(responses);
    }

    // 최근 기록 5개
    @GetMapping("/recent")
    public ResponseEntity<List<EmotionRecordResponse>> getRecentRecords(@LoginUser User user) {
        List<EmotionRecordResponse> responses = diaryService.getRecentRecords(user);
        return ResponseEntity.ok(responses);
    }
}