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

    // 누가 썼는지 연결 (Foreign Key)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "emotion_type", nullable = false)
    private String emotionType; // joy, sadness ...

    @Column(nullable = false)
    private Integer level; // 20, 40, 60...

    @Column(columnDefinition = "TEXT")
    private String reason; // 일기 내용

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt; // 사용자가 선택한 날짜

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt; // 실제 데이터 생성 시간
}