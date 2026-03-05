package com.team.backend.service;

import com.team.backend.dto.UserInfoResponse;
import com.team.backend.entity.User;
import com.team.backend.exception.ResourceNotFoundException;
import com.team.backend.repository.EmotionRecordRepository;
import com.team.backend.repository.SolutionLogRepository;
import com.team.backend.repository.SolutionRepository;
import com.team.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 정보 관련 비즈니스 로직 처리
 * - 사용자 정보 조회
 * - 회원 탈퇴
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final EmotionRecordRepository emotionRecordRepository;
    private final SolutionRepository solutionRepository;
    private final SolutionLogRepository solutionLogRepository;

    /**
     * 사용자 ID로 사용자 정보 조회
     */
    @Transactional(readOnly = true)
    public UserInfoResponse getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found - UserId: {}", userId);
                    return new ResourceNotFoundException("해당 사용자를 찾을 수 없습니다.");
                });

        log.info("User info retrieved - UserId: {}", userId);
        return UserInfoResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .age(user.getAge())
                .email(user.getEmail())
                .build();
    }

    /**
     * 회원 탈퇴 - 사용자의 모든 데이터를 순서대로 삭제 후 계정 삭제
     */
    @Transactional
    public void deleteUser(User user) {
        // 1. 해당 유저의 모든 감정 기록 조회
        var records = emotionRecordRepository.findAllByUser(user);

        // 2. 각 감정 기록에 연결된 SolutionLog, Solution 먼저 삭제 (참조 무결성 유지)
        for (var record : records) {
            solutionLogRepository.deleteAllByEmotionRecord(record);
            solutionRepository.deleteByEmotionRecord(record);
        }

        // 3. 감정 기록 전체 삭제
        emotionRecordRepository.deleteAll(records);

        // 4. 유저 삭제
        userRepository.delete(user);

        log.info("User account deleted - UserId: {}", user.getId());
    }
}
