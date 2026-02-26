package com.team.backend.dto;

import com.team.backend.entity.EmotionRecord;
import com.team.backend.entity.Solution;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class EmotionRecordResponse {
    private Long id;
    private String emotionType; // [수정 후] 이름을 emotionType으로 변경
    private Integer level;
    private LocalDateTime timestamp;
    private String reason;
    private SolutionInfo solution; // [추가] 솔루션 정보 (null 가능)

    @Getter
    @Builder
    public static class SolutionInfo {
        private Long id; // [추가] 솔루션 ID
        private String content;
        private Integer evaluation;
    }

    public static EmotionRecordResponse from(EmotionRecord record, Solution solutionEntity) {
        SolutionInfo solutionInfo = null;
        if (solutionEntity != null) {
            solutionInfo = SolutionInfo.builder()
                    .id(solutionEntity.getId())
                    .content(solutionEntity.getContent())
                    .evaluation(solutionEntity.getEvalScore())
                    .build();
        }

        return EmotionRecordResponse.builder()
                .id(record.getId())
                .emotionType(record.getEmotionType())
                .level(record.getLevel())
                .timestamp(record.getRecordedAt())
                .reason(record.getReason())
                .solution(solutionInfo) // 솔루션 데이터 세팅
                .build();
    }

    // 기존 메서드(솔루션 없는 경우) 호환성 유지
    public static EmotionRecordResponse from(EmotionRecord record) {
        return from(record, null);
    }
}