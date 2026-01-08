package com.team.backend.controller;

import com.team.backend.annotation.LoginUser; // 추가
import com.team.backend.dto.EmotionRecordRequest;
import com.team.backend.entity.User; // 추가
import com.team.backend.service.DiaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/records") // v1 추가
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    @PostMapping
    // @LoginUser가 붙어서, 이제 user 변수에 1번 유저가 자동으로 들어옴
    public ResponseEntity<String> createDiary(@LoginUser User user, @RequestBody EmotionRecordRequest request) {
        Long diaryId = diaryService.saveDiary(user, request);
        return ResponseEntity.ok("✅ 일기 저장 성공! ID: " + diaryId);
    }
}