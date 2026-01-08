package com.team.backend.controller;

import com.team.backend.dto.EmotionRecordRequest;
import com.team.backend.service.DiaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    @PostMapping
    public ResponseEntity<String> createDiary(@RequestBody EmotionRecordRequest request) {
        Long diaryId = diaryService.saveDiary(request);
        return ResponseEntity.ok("✅ 일기 저장 성공! 저장된 ID: " + diaryId);
    }
}