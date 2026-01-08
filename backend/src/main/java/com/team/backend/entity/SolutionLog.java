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
@Table(name = "SOLUTION_LOGS")
public class SolutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long id;

    // 어떤 일기에 대한 조언이었는지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id", nullable = false)
    private EmotionRecord emotionRecord;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content; // 제공했던 솔루션

    @Column(name = "eval_score")
    private Integer evalScore; // 당시 평가 점수

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}