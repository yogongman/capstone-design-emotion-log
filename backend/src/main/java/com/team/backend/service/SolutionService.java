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

        // 5. [프롬프트 구성] 가이드라인 + 과거 로그 + 현재 상황
        String finalPrompt = buildPromptWithFullHistory(currentRecord, similarRecords);

        // 6. Gemini 호출 (솔루션 생성)
        String aiReply = geminiService.generateSolution(finalPrompt);

        // 7. 결과 저장 (로직 변경)

        // 7-1. 화면 표시용 Solution 저장 (Upsert: 있으면 수정, 없으면 생성)
        Solution solution = solutionRepository.findByEmotionRecord(currentRecord)
                .orElse(null);

        if (solution != null) {
            // 이미 존재하면 -> 내용만 갈아끼우기 (Update)
            solution.updateContent(aiReply);
            // JPA의 Dirty Checking(변경 감지)에 의해 트랜잭션 종료 시 자동 update 쿼리 나감
        } else {
            // 없으면 -> 새로 만들기 (Insert)
            solution = Solution.builder()
                    .emotionRecord(currentRecord)
                    .content(aiReply)
                    .evalScore(0)
                    .build();
            solutionRepository.save(solution);
        }

        // 7-2. 학습 데이터용 SolutionLog 저장
        // 로그는 "히스토리" 개념이므로, 재생성할 때마다 계속 쌓는 게 맞습니다. (나중에 "이런 답변은 싫어했다"는 데이터로 활용 가능)
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

        solution.updateScore(score);

        // 학습 데이터용 로그도 업데이트 (가장 최근 로그라고 가정)
        solutionLogRepository.findTopByEmotionRecordOrderByCreatedAtDesc(solution.getEmotionRecord())
                .ifPresent(log -> log.updateScore(score));
    }

    // ==========================================
    // [Internal Methods]
    // ==========================================

    /**
     * In-Memory 코사인 유사도 계산 -> Top 10 추출
     */
    private List<EmotionRecord> findTop10SimilarRecords(User user, EmotionRecord current, String currentVector) {
        List<EmotionRecord> candidates = emotionRecordRepository.findAllByUserAndEmbeddingIsNotNullAndIdNot(user, current.getId());

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        Map<EmotionRecord, Double> scoreMap = new HashMap<>();
        for (EmotionRecord target : candidates) {
            double score = geminiService.calculateCosineSimilarity(currentVector, target.getEmbedding());
            scoreMap.put(target, score);
        }

        return scoreMap.entrySet().stream()
                .sorted(Map.Entry.<EmotionRecord, Double>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 프롬프트 조립 (핵심: 행동 제안 가이드라인 추가)
     */
    private String buildPromptWithFullHistory(EmotionRecord current, List<EmotionRecord> similarRecords) {
        StringBuilder prompt = new StringBuilder();

        // 1. 페르소나 및 기본 역할
        prompt.append("[시스템 지시사항]\n");
        prompt.append("당신은 따뜻한 공감 능력과 문제 해결 능력을 겸비한 '라이프 코치'입니다.\n");
        prompt.append("사용자의 일기를 읽고, 공감과 함께 **'지금 당장 실천할 수 있는 구체적인 행동(Action Item)'**을 포함하여 답변해주세요.\n\n");

        // 2. [New] 행동 제안 가이드라인 (맥락에 맞게 변형 유도)
        prompt.append("[행동 제안 가이드라인 (참고용)]\n");
        prompt.append("사용자의 감정 상태에 따라 아래와 같은 '해결 방향'을 참고하되, **반드시 일기 속 구체적인 상황(장소, 시간, 사건)에 맞춰 자연스럽게 변형**하여 제안하세요.\n");
        prompt.append("- 기쁨(joy)/평온(calm): 이 순간을 사진, 메모, 음악 등으로 '기록'하거나 '저장'하도록 유도.\n");
        prompt.append("- 슬픔(sadness): 거창한 해결보다는 따뜻한 차, 산책, 환기 등 기분을 전환할 수 있는 '작은 셀프 케어' 제안.\n");
        prompt.append("- 화남(anger): 화를 억누르지 말고, 안전하게 에너지를 배출하거나 잠시 자리를 피해서 '열을 식히는 행동' 제안.\n");
        prompt.append("- 긴장(anxiety): 복잡한 생각 끊기. 심호흡, 주변 사물 관찰하기 등 지금 이 순간 감각에 집중하는 '그라운딩(Grounding)' 제안.\n\n");

        prompt.append("[주의사항]\n");
        prompt.append("1. 앵무새처럼 위 예시를 그대로 읊지 마세요. (예: 회사에 있는 사람에게 '이불 속에 들어가라'고 하지 말 것)\n");
        prompt.append("2. 답변은 **두 문장 이내**로 짧고 간결하게 작성하세요.\n\n");

        // 3. 과거 데이터 주입 (RAG)
        prompt.append("[참고: 이 사용자의 과거 상담 이력 (유사한 상황)]\n");
        prompt.append("높은 점수(4~5점)를 받은 조언 스타일은 적극 참고하고, 낮은 점수(1~2점)를 받은 조언 스타일은 피해주세요.\n");

        boolean hasHistory = false;
        for (EmotionRecord record : similarRecords) {
            List<SolutionLog> logs = solutionLogRepository.findAllByEmotionRecord(record);
            for (SolutionLog logData : logs) {
                if (logData.getEvalScore() > 0) {
                    prompt.append("- 조언: \"").append(logData.getContent()).append("\"\n");
                    prompt.append("  (평가: ").append(logData.getEvalScore()).append("점)\n");
                    hasHistory = true;
                }
            }
        }

        if (!hasHistory) {
            prompt.append("(과거 이력 없음 - 가이드라인에 맞춰 최적의 답변을 해주세요.)\n");
        }
        prompt.append("\n");

        // 4. 현재 상황 입력
        prompt.append("[현재 사용자의 상황]\n");
        prompt.append("- 감정: ").append(current.getEmotionType()).append("\n");
        prompt.append("- 감정 강도(0~100): ").append(current.getLevel()).append("\n");
        prompt.append("- 일기 내용: \"").append(current.getReason()).append("\"\n\n");

        // 5. 답변 요청
        prompt.append("[답변 작성]\n");
        prompt.append("위 내용을 바탕으로 사용자에게 가장 필요한 위로와 행동 지침을 건네주세요.\n");
        prompt.append("답변:");

        return prompt.toString();
    }
}