package com.team.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class EmotionRecordRequest {
    private String emotionType; // "joy", "sadness"...
    private Integer level;      // 20, 40...
    private String reason;      // "오늘 너무 힘들었다..."
    private LocalDateTime recordedAt; // (선택) 없으면 현재시간
}