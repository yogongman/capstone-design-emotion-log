package com.team.backend.repository;

import com.team.backend.entity.EmotionRecord;
import com.team.backend.entity.Solution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SolutionRepository extends JpaRepository<Solution, Long> {
    Optional<Solution> findByEmotionRecord(EmotionRecord emotionRecord);

    // [추가] 특정 일기에 연결된 솔루션 삭제
    void deleteByEmotionRecord(EmotionRecord emotionRecord);
}