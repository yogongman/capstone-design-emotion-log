package com.team.backend.repository;

import com.team.backend.entity.EmotionRecord;
import com.team.backend.entity.SolutionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolutionLogRepository extends JpaRepository<SolutionLog, Long> {
    // 특정 일기에 달린 모든 솔루션 로그 가져오기
    List<SolutionLog> findAllByEmotionRecord(EmotionRecord emotionRecord);
}