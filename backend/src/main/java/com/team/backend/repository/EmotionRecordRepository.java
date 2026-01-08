package com.team.backend.repository;

import com.team.backend.entity.EmotionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmotionRecordRepository extends JpaRepository<EmotionRecord, Long> {
    // 필요한 쿼리 메서드가 있다면 여기에 추가 (지금은 기본 기능만 있으면 됨)
}