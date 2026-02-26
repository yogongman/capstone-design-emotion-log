package com.team.backend.repository;

import com.team.backend.entity.EmotionRecord;
import com.team.backend.entity.SolutionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolutionLogRepository extends JpaRepository<SolutionLog, Long> {
    List<SolutionLog> findAllByEmotionRecord(EmotionRecord emotionRecord);

    // [추가] 특정 일기에 연결된 모든 로그 삭제
    void deleteAllByEmotionRecord(EmotionRecord emotionRecord);

    // [추가] 가장 최근에 생성된 해당 일기의 로그 하나 가져오기
    java.util.Optional<SolutionLog> findTopByEmotionRecordOrderByCreatedAtDesc(EmotionRecord emotionRecord);
}