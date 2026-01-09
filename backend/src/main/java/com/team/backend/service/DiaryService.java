package com.team.backend.service;

import com.team.backend.dto.EmotionRecordRequest;
import com.team.backend.entity.EmotionRecord;
import com.team.backend.entity.User;
import com.team.backend.repository.EmotionRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DiaryService {

    private final EmotionRecordRepository emotionRecordRepository;

    @Transactional
    public Long saveDiary(User user, EmotionRecordRequest request) {
        EmotionRecord record = EmotionRecord.builder()
                .user(user)
                .emotionType(request.getEmotionType())
                .level(request.getLevel())
                .reason(request.getReason())
                .recordedAt(request.getRecordedAt() != null ? request.getRecordedAt() : java.time.LocalDateTime.now())
                .build();

        return emotionRecordRepository.save(record).getId();
    }

    @Transactional
    public void updateDiary(User user, Long recordId, EmotionRecordRequest request) {
        EmotionRecord record = emotionRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("해당 일기가 존재하지 않습니다. ID=" + recordId));

        validateOwnership(record, user);

        record.update(request.getEmotionType(), request.getLevel(), request.getReason());
    }

    @Transactional
    public void deleteDiary(User user, Long recordId) {
        // 1. 일기 찾기
        EmotionRecord record = emotionRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("해당 일기가 존재하지 않습니다. ID=" + recordId));

        // 2. 본인 확인
        validateOwnership(record, user);

        // 3. 삭제
        emotionRecordRepository.delete(record);
    }

    // [내부 검증 메서드]
    private void validateOwnership(EmotionRecord record, User user) {
        if (!record.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("작성자만 수정/삭제할 수 있습니다.");
        }
    }
}