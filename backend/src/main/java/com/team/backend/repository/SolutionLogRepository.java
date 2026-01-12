package com.team.backend.repository;

import com.team.backend.entity.EmotionRecord;
import com.team.backend.entity.SolutionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolutionLogRepository extends JpaRepository<SolutionLog, Long> {
    List<SolutionLog> findAllByEmotionRecord(EmotionRecord emotionRecord);

    // [추가] 특정 일기에 연결된 모든 로그 삭제
    void deleteAllByEmotionRecord(EmotionRecord emotionRecord);
}