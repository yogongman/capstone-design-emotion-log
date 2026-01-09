package com.team.backend.repository;

import com.team.backend.entity.EmotionRecord;
import com.team.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EmotionRecordRepository extends JpaRepository<EmotionRecord, Long> {

    // [추가] 특정 유저의 특정 기간(start ~ end) 기록 조회 (최신순 정렬)
    List<EmotionRecord> findAllByUserAndRecordedAtBetweenOrderByRecordedAtDesc(
            User user,
            LocalDateTime start,
            LocalDateTime end
    );
    // [추가] 최근 기록 5개 조회 (Top5)
    List<EmotionRecord> findTop5ByUserOrderByRecordedAtDesc(User user);
}