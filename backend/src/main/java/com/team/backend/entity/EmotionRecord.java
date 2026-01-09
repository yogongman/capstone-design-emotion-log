package com.team.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "EMOTION_RECORDS")
public class EmotionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "emotion_type", nullable = false)
    private String emotionType;

    @Column(nullable = false)
    private Integer level;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

     public void update(String emotionType, Integer level, String reason) {
        if (emotionType != null) this.emotionType = emotionType;
        if (level != null) this.level = level;
        if (reason != null) this.reason = reason;
        // recordedAt은 수정 불가 (정책상)
    }
}