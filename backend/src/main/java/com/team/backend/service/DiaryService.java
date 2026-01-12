package com.team.backend.service;

import com.team.backend.dto.EmotionRecordRequest;
import com.team.backend.dto.EmotionRecordResponse;
import com.team.backend.entity.EmotionRecord;
import com.team.backend.entity.Solution;
import com.team.backend.entity.User;
import com.team.backend.repository.EmotionRecordRepository;
import com.team.backend.repository.SolutionRepository; // [추가]
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiaryService {

    private final EmotionRecordRepository emotionRecordRepository;
    private final SolutionRepository solutionRepository; // [추가]

    // 기록 저장
    @Transactional
    public Long saveDiary(User user, EmotionRecordRequest request) {
        EmotionRecord record = EmotionRecord.builder()
                .user(user)
                .emotionType(request.getEmotionType())
                .level(request.getLevel())
                .reason(request.getReason())
                .recordedAt(request.getRecordedAt() != null ? request.getRecordedAt() : java.time.LocalDateTime.now())
                .build();

        return emotionRecordRepository.save(record).getId();
    }

    // 기록 수정
    @Transactional
    public void updateDiary(User user, Long recordId, EmotionRecordRequest request) {
        EmotionRecord record = emotionRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("해당 일기가 존재하지 않습니다. ID=" + recordId));

        validateOwnership(record, user);

        record.update(request.getEmotionType(), request.getLevel(), request.getReason());
    }

    // 기록 삭제
    @Transactional
    public void deleteDiary(User user, Long recordId) {
        // 1. 일기 찾기
        EmotionRecord record = emotionRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("해당 일기가 존재하지 않습니다. ID=" + recordId));

        // 2. 본인 확인
        validateOwnership(record, user);

        // 3. 삭제
        emotionRecordRepository.delete(record);
    }

    // [내부 검증 메서드]
    private void validateOwnership(EmotionRecord record, User user) {
        if (!record.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("작성자만 수정/삭제할 수 있습니다.");
        }
    }

    // 월간 기록
    @Transactional(readOnly = true)
    public List<EmotionRecordResponse> getMonthlyRecords(User user, int year, int month) {

        // 1. 날짜 범위 계산 (예: 2025-11 -> 2025-11-01 00:00 ~ 2025-11-30 23:59:59)
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        // 2. DB 조회
        List<EmotionRecord> records = emotionRecordRepository.findAllByUserAndRecordedAtBetweenOrderByRecordedAtDesc(user, start, end);

        // 3. 엔티티 리스트 -> DTO 리스트 변환
        return records.stream()
                .map(EmotionRecordResponse::from)
                .collect(Collectors.toList());
    }

    // 일간 상세 조회
    @Transactional(readOnly = true)
    public List<EmotionRecordResponse> getDailyRecords(User user, String dateStr) {
        // 1. 날짜 파싱 (String "2025-11-27" -> LocalDate)
        LocalDate date = LocalDate.parse(dateStr);

        // 2. 시간 범위 설정 (00:00:00 ~ 23:59:59)
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(23, 59, 59);

        // 3. 해당 날짜의 일기 조회
        List<EmotionRecord> records = emotionRecordRepository.findAllByUserAndRecordedAtBetweenOrderByRecordedAtDesc(user, start, end);

        // 4. 일기마다 솔루션 찾아서 DTO 변환
        return records.stream()
                .map(record -> {
                    // 해당 일기에 연결된 솔루션이 있는지 조회
                    Solution solution = solutionRepository.findByEmotionRecord(record).orElse(null);
                    // DTO 변환 (솔루션 포함)
                    return EmotionRecordResponse.from(record, solution);
                })
                .collect(Collectors.toList());
    }

    // 홈화면 최근 기록 5개 조회
    @Transactional(readOnly = true)
    public List<EmotionRecordResponse> getRecentRecords(User user) {
        // 1. Top 5 조회
        List<EmotionRecord> records = emotionRecordRepository.findTop5ByUserOrderByRecordedAtDesc(user);

        // 2. DTO 변환 (최근 기록에는 보통 솔루션 상세까지는 필요 없어서 기본 from 사용)
        return records.stream()
                .map(EmotionRecordResponse::from)
                .collect(Collectors.toList());
    }
}