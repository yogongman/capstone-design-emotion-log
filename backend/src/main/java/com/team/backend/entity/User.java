package com.team.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity // 이 클래스가 곧 DB 테이블이라는 뜻
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "USERS") // 'USER'는 DB 예약어라 에러가 잘 나서 'USERS'로 지정
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(name = "social_id", unique = true, nullable = false)
    private String socialId;

    private String nickname;

    private Integer age;

    private String gender;

    @CreationTimestamp // 자동으로 현재 시간 저장
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}