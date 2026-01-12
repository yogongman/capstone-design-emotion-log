package com.team.backend.service;

import com.team.backend.dto.SolutionResponse;
import com.team.backend.entity.*;
import com.team.backend.repository.EmotionRecordRepository;
import com.team.backend.repository.SolutionLogRepository;
import com.team.backend.repository.SolutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SolutionService {

    private final EmotionRecordRepository emotionRecordRepository;
    private final SolutionRepository solutionRepository;
    private final SolutionLogRepository solutionLogRepository;
    private final GeminiService geminiService;

    /**
     * 1. 솔루션 생성 (RAG: Retrieval-Augmented Generation)
     */
    @Transactional
    public SolutionResponse generateSolution(User user, Long recordId) {
        // 1. 현재 일기 조회
        EmotionRecord currentRecord = emotionRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("일기를 찾을 수 없습니다."));

        if (!currentRecord.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("본인의 일기에만 솔루션을 생성할 수 있습니다.");
        }

        // 2. 현재 일기의 텍스트 임베딩 생성 (목적: 검색용 Query)
        String contentForEmbedding = "Emotion: " + currentRecord.getEmotionType() +
                ", Content: " + currentRecord.getReason();

        // Gemini에게 "이것은 검색을 위한 쿼리(RETRIEVAL_QUERY)다"라고 명시
        String currentEmbedding = geminiService.getEmbedding(contentForEmbedding, "RETRIEVAL_QUERY");

        // 3. 임베딩 DB에 저장 (나중에 검색될 문서(RETRIEVAL_DOCUMENT)로 쓰기 위해)
        currentRecord.updateEmbedding(currentEmbedding);

        // 4. [RAG 핵심] 유사도 기반 과거 기록 Top 10 찾기
        List<EmotionRecord> similarRecords = findTop10SimilarRecords(user, currentRecord, currentEmbedding);

        // 5. [프롬프트 구성] 과거 로그(점수 포함) + 현재 상황 -> 한국어 프롬프트 생성
        String finalPrompt = buildPromptWithFullHistory(currentRecord, similarRecords);

        // 6. Gemini 호출 (솔루션 생성)
        String aiReply = geminiService.generateSolution(finalPrompt);

        // 7. 결과 저장
        // 7-1. 화면 표시용 Solution 저장
        Solution solution = Solution.builder()
                .emotionRecord(currentRecord)
                .content(aiReply)
                .evalScore(0) // 초기값 0점
                .build();
        solutionRepository.save(solution);

        // 7-2. 학습 데이터용 SolutionLog 저장
        SolutionLog log = SolutionLog.builder()
                .emotionRecord(currentRecord)
                .content(aiReply)
                .evalScore(0)
                .build();
        solutionLogRepository.save(log);

        return SolutionResponse.builder()
                .solutionId(solution.getId())
                .content(solution.getContent())
                .build();
    }

    /**
     * 2. 솔루션 평가 (Feedback)
     */
    @Transactional
    public void evaluateSolution(User user, Long solutionId, Integer score) {
        Solution solution = solutionRepository.findById(solutionId)
                .orElseThrow(() -> new IllegalArgumentException("솔루션이 없습니다."));

        if (!solution.getEmotionRecord().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("본인의 솔루션만 평가할 수 있습니다.");
        }

        // 점수 업데이트 (Entity에 updateScore 메서드 필요)
        solution.updateScore(score);

        // (선택사항) SolutionLog도 같이 찾아서 업데이트해주면 더 좋음
        // 여기서는 편의상 생략하지만, 실제 서비스면 Log도 동기화해야 함.
    }

    // ==========================================
    // [Internal Methods]
    // ==========================================

    /**
     * In-Memory 코사인 유사도 계산 -> Top 10 추출
     */
    private List<EmotionRecord> findTop10SimilarRecords(User user, EmotionRecord current, String currentVector) {
        // 1. 임베딩이 있는 내 과거 일기 다 가져오기 (본인 글 제외)
        List<EmotionRecord> candidates = emotionRecordRepository.findAllByUserAndEmbeddingIsNotNullAndIdNot(user, current.getId());

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 점수 매기기
        Map<EmotionRecord, Double> scoreMap = new HashMap<>();
        for (EmotionRecord target : candidates) {
            double score = geminiService.calculateCosineSimilarity(currentVector, target.getEmbedding());
            scoreMap.put(target, score);
        }

        // 3. 점수 높은 순 정렬 & 상위 10개 자르기
        return scoreMap.entrySet().stream()
                .sorted(Map.Entry.<EmotionRecord, Double>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 프롬프트 조립 (한국어, 모든 로그 포함)
     */
    private String buildPromptWithFullHistory(EmotionRecord current, List<EmotionRecord> similarRecords) {
        StringBuilder prompt = new StringBuilder();

        // 1. 페르소나 및 역할 부여
        prompt.append("[시스템 지시사항]\n");
        prompt.append("당신은 따뜻하고 공감 능력이 뛰어난 심리 상담가입니다.\n");
        prompt.append("사용자의 일기 내용을 읽고, 따뜻한 위로와 실질적인 조언이 담긴 \"한 문장의 솔루션\"을 한국어로 작성해주세요.\n\n");

        // 2. 과거 데이터 주입 (유사도 Top 10의 모든 로그)
        prompt.append("[참고: 이 사용자의 과거 상담 이력 (유사한 상황)]\n");
        prompt.append("아래는 사용자가 과거에 비슷한 상황에서 받았던 조언과 그에 대한 평가 점수(1~5점)입니다.\n");
        prompt.append("높은 점수(4~5점)를 받은 조언 스타일은 적극 참고하고, 낮은 점수(1~2점)를 받은 조언 스타일은 피해주세요.\n\n");

        boolean hasHistory = false;

        for (EmotionRecord record : similarRecords) {
            // 해당 기록에 달린 솔루션 로그 조회
            List<SolutionLog> logs = solutionLogRepository.findAllByEmotionRecord(record);

            for (SolutionLog logData : logs) {
                // 평가가 없는(0점) 로그는 굳이 학습 데이터로 안 써도 됨 (선택사항)
                // 여기서는 평가된 것만 넣겠습니다.
                if (logData.getEvalScore() > 0) {
                    prompt.append("- 조언: \"").append(logData.getContent()).append("\"\n");
                    prompt.append("  (평가: ").append(logData.getEvalScore()).append("점)\n");
                    hasHistory = true;
                }
            }
        }

        if (!hasHistory) {
            prompt.append("(과거 이력 없음 - 보편적으로 좋은 상담을 해주세요.)\n");
        }
        prompt.append("\n");

        // 3. 현재 상황 입력
        prompt.append("[현재 사용자의 상황]\n");
        prompt.append("- 감정: ").append(current.getEmotionType()).append("\n");
        prompt.append("- 감정 강도(0~100): ").append(current.getLevel()).append("\n");
        prompt.append("- 일기 내용: \"").append(current.getReason()).append("\"\n\n");

        // 4. 답변 요청
        prompt.append("[답변 작성]\n");
        prompt.append("위 내용을 바탕으로 사용자에게 가장 적절한 위로를 건네주세요.\n");
        prompt.append("답변:");

        return prompt.toString();
    }
}