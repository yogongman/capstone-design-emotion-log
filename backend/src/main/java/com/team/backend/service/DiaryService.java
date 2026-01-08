package com.team.backend.service;

import com.team.backend.dto.EmotionRecordRequest;
import com.team.backend.entity.EmotionRecord;
import com.team.backend.entity.User;
import com.team.backend.repository.EmotionRecordRepository;
import com.team.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DiaryService {

    private final EmotionRecordRepository emotionRecordRepository;
    private final UserRepository userRepository;

    @Transactional
    public Long saveDiary(EmotionRecordRequest request) {
        // 1. 임시로 1번 유저 고정 (DB에 아까 넣은 그 유저)
        User user = userRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("테스트 유저가 없습니다. SQL 실행하세요!"));

        // 2. 엔티티 변환 및 저장
        EmotionRecord record = EmotionRecord.builder()
                .user(user)
                .emotionType(request.getEmotionType())
                .level(request.getLevel())
                .reason(request.getReason())
                .recordedAt(request.getRecordedAt() != null ? request.getRecordedAt() : java.time.LocalDateTime.now())
                .build();

        return emotionRecordRepository.save(record).getId();
    }
}