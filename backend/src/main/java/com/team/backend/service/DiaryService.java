package com.team.backend.service;

import com.team.backend.dto.EmotionRecordRequest;
import com.team.backend.dto.EmotionRecordResponse;
import com.team.backend.entity.EmotionRecord;
import com.team.backend.entity.Solution;
import com.team.backend.entity.User;
import com.team.backend.repository.EmotionRecordRepository;
import com.team.backend.repository.SolutionLogRepository; // [추가]
import com.team.backend.repository.SolutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiaryService {

    private final EmotionRecordRepository emotionRecordRepository;
    private final SolutionRepository solutionRepository;
    private final SolutionLogRepository solutionLogRepository; // [추가]
    private final GeminiService geminiService; // [추가]

    // 기록 저장
    @Transactional
    public Long saveDiary(User user, EmotionRecordRequest request) {
        // 1. 임베딩 생성 (저장용이므로 RETRIEVAL_DOCUMENT)
        // 검색 품질을 높이기 위해 감정 타입과 내용을 조합해서 벡터화
        String contentForEmbedding = "Emotion: " + request.getEmotionType() + ", Content: " + request.getReason();
        String embedding = geminiService.getEmbedding(contentForEmbedding, "RETRIEVAL_DOCUMENT");

        // 2. 일기 저장 (임베딩 포함)
        EmotionRecord record = EmotionRecord.builder()
                .user(user)
                .emotionType(request.getEmotionType())
                .level(request.getLevel())
                .reason(request.getReason())
                .embedding(embedding) // [추가]
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

        // [중요] 내용(reason)이 변경되었는지 확인
        // 내용이 바뀌었다면 -> 임베딩도 바뀌어야 하고 -> 과거 솔루션은 의미가 없어지므로 삭제해야 함.
        if (!record.getReason().equals(request.getReason())) {
            // 1. 새로운 임베딩 생성
            String contentForEmbedding = "Emotion: " + request.getEmotionType() + ", Content: " + request.getReason();
            String newEmbedding = geminiService.getEmbedding(contentForEmbedding, "RETRIEVAL_DOCUMENT");

            // 2. 임베딩 업데이트
            record.updateEmbedding(newEmbedding);

            // 3. 연관된 과거 데이터 삭제 (오염 방지)
            // FK 제약 조건 때문에 로그(Child)를 먼저 지우고 솔루션(Parent)을 지우거나, 순서대로 삭제
            solutionLogRepository.deleteAllByEmotionRecord(record); // 로그 삭제
            solutionRepository.deleteByEmotionRecord(record);       // 현재 솔루션 삭제
        }

        // 4. 나머지 필드 업데이트
        record.update(request.getEmotionType(), request.getLevel(), request.getReason());
    }

    // 기록 삭제
    @Transactional
    public void deleteDiary(User user, Long recordId) {
        EmotionRecord record = emotionRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("해당 일기가 존재하지 않습니다. ID=" + recordId));

        validateOwnership(record, user);

        // Cascade 설정이 되어 있다면 record 삭제 시 자동 삭제되겠지만,
        // 명시적으로 안전하게 연관 데이터를 먼저 지워주는 것이 좋음 (선택 사항)
        solutionLogRepository.deleteAllByEmotionRecord(record);
        solutionRepository.deleteByEmotionRecord(record);

        emotionRecordRepository.delete(record);
    }

    // ... (이하 validateOwnership, 조회 메서드들은 기존 코드 유지) ...
    // [내부 검증 메서드]
    private void validateOwnership(EmotionRecord record, User user) {
        if (!record.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("작성자만 수정/삭제할 수 있습니다.");
        }
    }

    // 월간 조회
    @Transactional(readOnly = true)
    public List<EmotionRecordResponse> getMonthlyRecords(User user, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        List<EmotionRecord> records = emotionRecordRepository.findAllByUserAndRecordedAtBetweenOrderByRecordedAtDesc(user, start, end);

        return records.stream()
                .map(record -> {
                    Solution solution = solutionRepository.findByEmotionRecord(record).orElse(null);
                    return EmotionRecordResponse.from(record, solution);
                })
                .collect(Collectors.toList());
    }

    // 일간 조회
    @Transactional(readOnly = true)
    public List<EmotionRecordResponse> getDailyRecords(User user, String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(23, 59, 59);

        List<EmotionRecord> records = emotionRecordRepository.findAllByUserAndRecordedAtBetweenOrderByRecordedAtDesc(user, start, end);

        return records.stream()
                .map(record -> {
                    Solution solution = solutionRepository.findByEmotionRecord(record).orElse(null);
                    return EmotionRecordResponse.from(record, solution);
                })
                .collect(Collectors.toList());
    }

    // 최근 기록 조회
    @Transactional(readOnly = true)
    public List<EmotionRecordResponse> getRecentRecords(User user) {
        List<EmotionRecord> records = emotionRecordRepository.findTop5ByUserOrderByRecordedAtDesc(user);
        return records.stream()
                .map(record -> {
                    Solution solution = solutionRepository.findByEmotionRecord(record).orElse(null);
                    return EmotionRecordResponse.from(record, solution);
                })
                .collect(Collectors.toList());
    }
}