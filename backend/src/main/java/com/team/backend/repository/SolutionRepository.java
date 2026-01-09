package com.team.backend.repository;

import com.team.backend.entity.EmotionRecord;
import com.team.backend.entity.Solution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SolutionRepository extends JpaRepository<Solution, Long> {
    // 특정 일기(Record)에 달린 솔루션을 찾는 메서드
    Optional<Solution> findByEmotionRecord(EmotionRecord emotionRecord);
}