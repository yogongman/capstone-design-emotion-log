package com.team.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "SOLUTIONS")
public class Solution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "solution_id")
    private Long id;

    // 일기 하나당 솔루션 하나 (1:1 관계)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id", unique = true, nullable = false)
    private EmotionRecord emotionRecord;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content; // AI가 해준 조언 내용

    @Column(name = "eval_score")
    private Integer evalScore; // 사용자가 매긴 점수 (1~5)

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp // 수정될 때마다 시간 갱신
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 점수 업데이트 편의 메서드
    public void updateScore(Integer score) {
        this.evalScore = score;
    }
}