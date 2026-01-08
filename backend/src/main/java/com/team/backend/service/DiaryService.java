package com.team.backend.service;

import com.team.backend.dto.EmotionRecordRequest;
import com.team.backend.entity.EmotionRecord;
import com.team.backend.entity.User;
import com.team.backend.repository.EmotionRecordRepository;
// UserRepository import 제거 (이제 안 씀)
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DiaryService {

    private final EmotionRecordRepository emotionRecordRepository;

    // UserRepository 의존성 제거됨

    @Transactional
    public Long saveDiary(User user, EmotionRecordRequest request) { // 파라미터로 User 받음
        // 1. 하드코딩 삭제됨! 컨트롤러가 넘겨준 user를 바로 사용

        // 2. 엔티티 변환 및 저장
        EmotionRecord record = EmotionRecord.builder()
                .user(user)
                .emotionType(request.getEmotionType())
                .level(request.getLevel())
                .reason(request.getReason())
                // 날짜 없으면 오늘 날짜로
                .recordedAt(request.getRecordedAt() != null ? request.getRecordedAt() : java.time.LocalDateTime.now())
                .build();

        return emotionRecordRepository.save(record).getId();
    }
}