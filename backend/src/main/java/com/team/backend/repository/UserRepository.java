package com.team.backend.repository;

import com.team.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // 나중에 소셜 로그인할 때 쓸 메서드 미리 추가
    Optional<User> findBySocialId(String socialId);
}