package com.team.backend.controller;

import com.team.backend.annotation.LoginUser;
import com.team.backend.dto.EmotionRecordRequest;
import com.team.backend.entity.User;
import com.team.backend.service.DiaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/records")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    @PostMapping
    public ResponseEntity<String> createDiary(@LoginUser User user, @RequestBody EmotionRecordRequest request) {
        Long diaryId = diaryService.saveDiary(user, request);
        return ResponseEntity.ok("✅ 일기 저장 성공! ID: " + diaryId);
    }

    @PatchMapping("/{recordId}")
    public ResponseEntity<Map<String, Boolean>> updateDiary(
            @LoginUser User user,
            @PathVariable Long recordId,
            @RequestBody EmotionRecordRequest request
    ) {
        diaryService.updateDiary(user, recordId, request);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @DeleteMapping("/{recordId}")
    public ResponseEntity<Map<String, Boolean>> deleteDiary(
            @LoginUser User user,
            @PathVariable Long recordId
    ) {
        diaryService.deleteDiary(user, recordId);
        // 명세서 기준 { "success": true } 반환
        return ResponseEntity.ok(Map.of("success", true));
    }
}