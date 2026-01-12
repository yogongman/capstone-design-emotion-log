package com.team.backend.repository;

import com.team.backend.entity.EmotionRecord;
import com.team.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EmotionRecordRepository extends JpaRepository<EmotionRecord, Long> {

    // 특정 유저의 특정 기간(start ~ end) 기록 조회 (최신순 정렬)
    List<EmotionRecord> findAllByUserAndRecordedAtBetweenOrderByRecordedAtDesc(
            User user,
            LocalDateTime start,
            LocalDateTime end
    );
    // 최근 기록 5개 조회 (Top5)
    List<EmotionRecord> findTop5ByUserOrderByRecordedAtDesc(User user);


    //임베딩이 있는 내 과거 기록들 모두 조회 (유사도 계산용)
    // 단, 현재 작성 중인 일기(targetId)는 제외하고 가져옴
    List<EmotionRecord> findAllByUserAndEmbeddingIsNotNullAndIdNot(User user, Long recordId);
}