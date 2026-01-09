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

    // 1. 저장 (명세서 포맷에 맞춰 JSON 반환으로 수정)
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

    // 2. 수정 (기존과 동일, 명세서 준수)
    @PatchMapping("/{recordId}")
    public ResponseEntity<Map<String, Boolean>> updateDiary(
            @LoginUser User user,
            @PathVariable Long recordId,
            @RequestBody EmotionRecordRequest request
    ) {
        diaryService.updateDiary(user, recordId, request);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // 3. 삭제 (기존과 동일, 명세서 준수)
    @DeleteMapping("/{recordId}")
    public ResponseEntity<Map<String, Boolean>> deleteDiary(
            @LoginUser User user,
            @PathVariable Long recordId
    ) {
        diaryService.deleteDiary(user, recordId);
        return ResponseEntity.ok(Map.of("success", true));
    }
}