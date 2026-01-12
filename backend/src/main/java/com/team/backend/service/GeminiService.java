package com.team.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key}")
    private String apiKey;

    private static final String MODEL_EMBEDDING = "gemini-embedding-001";
    private static final String MODEL_CHAT = "gemini-2.5-flash"; // 또는 "gemini-2.5-flash"

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    /**
     * 1. 텍스트 임베딩 가져오기
     */
    public String getEmbedding(String text, String taskType) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("content", Map.of("parts", List.of(Map.of("text", text))));
        if (taskType != null) requestBody.put("taskType", taskType);

        try {
            String response = webClientBuilder.build()
                    .post()
                    .uri(BASE_URL + MODEL_EMBEDDING + ":embedContent")
                    .header("x-goog-api-key", apiKey) // 👈 [변경] 헤더 인증 방식 적용
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            return root.path("embedding").path("values").toString();

        } catch (Exception e) {
            log.error("Gemini Embedding Error", e);
            throw new RuntimeException("임베딩 생성 실패");
        }
    }

    /**
     * 2. 솔루션 생성하기 (채팅)
     */
    public String generateSolution(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        try {
            String response = webClientBuilder.build()
                    .post()
                    .uri(BASE_URL + MODEL_CHAT + ":generateContent")
                    .header("x-goog-api-key", apiKey) // 👈 [변경] 헤더 인증 방식 적용
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode candidates = root.path("candidates");

            if (candidates.isEmpty()) return "죄송해요, 답변을 생성하지 못했어요.";

            return candidates.get(0).path("content").path("parts").get(0).path("text").asText();

        } catch (Exception e) {
            log.error("Gemini Chat Error", e);
            return "AI 서비스 연결 오류";
        }
    }

    // ... calculateCosineSimilarity는 그대로 유지 ...
    public double calculateCosineSimilarity(String vectorA, String vectorB) {
        try {
            if (vectorA == null || vectorB == null) return 0.0;
            double[] v1 = objectMapper.readValue(vectorA, double[].class);
            double[] v2 = objectMapper.readValue(vectorB, double[].class);
            if (v1.length != v2.length) return 0.0;
            double dot = 0.0, nA = 0.0, nB = 0.0;
            for (int i = 0; i < v1.length; i++) {
                dot += v1[i] * v2[i];
                nA += Math.pow(v1[i], 2);
                nB += Math.pow(v2[i], 2);
            }
            return (nA == 0 || nB == 0) ? 0.0 : dot / (Math.sqrt(nA) * Math.sqrt(nB));
        } catch (JsonProcessingException e) {
            return 0.0;
        }
    }
}