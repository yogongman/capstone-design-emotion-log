package com.team.backend.service;

import com.team.backend.dto.UserInfoResponse;
import com.team.backend.entity.User;
import com.team.backend.exception.ResourceNotFoundException;
import com.team.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 정보 관련 비즈니스 로직 처리
 * - 사용자 정보 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * 사용자 ID로 사용자 정보 조회
     * @param userId 사용자 ID
     * @return UserInfoResponse (id, nickname, age, email)
     * @throws ResourceNotFoundException 사용자가 존재하지 않을 때
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
}
